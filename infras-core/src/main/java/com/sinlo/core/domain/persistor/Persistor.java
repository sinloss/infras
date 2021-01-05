package com.sinlo.core.domain.persistor;

import com.sinlo.core.common.util.Eveny;
import com.sinlo.core.domain.persistor.spec.Entity;
import com.sinlo.core.domain.persistor.spec.Repo;
import com.sinlo.core.domain.persistor.spec.Selector;
import com.sinlo.core.domain.persistor.spec.Tag;
import com.sinlo.core.domain.persistor.util.ReposSelector;
import com.sinlo.core.common.wraparound.SureThreadLocal;
import com.sinlo.sponte.util.Pool;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Persistor the persistor who persists tagged entities, this is specifically designed to
 * tackle the entity persisting problems inside of aggregate roots
 *
 * @author sinlo
 */
public class Persistor<T extends Entity> {

    private static final Pool.Simple<Persistor<?>> pool = new Pool.Simple<>();

    private final SureThreadLocal<List<String>> tagged = SureThreadLocal.of(LinkedList::new);
    private final SureThreadLocal<Map<String, Tag<T>>> entities = SureThreadLocal.of(HashMap::new);

    private final Eveny<State, Tag<T>> fine = new Eveny<>();
    private final Eveny<State, Void> rough = new Eveny<>();
    private final Eveny<Persistor<T>, Tag<T>.Ex> except = new Eveny<>();

    private Persistor() {
    }

    /**
     * Get or create the persistor if none exists regarding the given entity class
     */
    @SuppressWarnings("unchecked")
    public static <T extends Entity> Persistor<T> of(Class<T> clz) {
        return (Persistor<T>) pool.get(clz.getName(), Persistor::new);
    }

    /**
     * Find the first persistor matches the given class or any of its ancestors
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Entity> Persistor<T> find(Class<T> clz) {
        if (!Entity.class.equals(clz)) {
            Persistor pooled = pool.get(clz.getName());
            if (pooled == null) {
                return find((Class<T>) clz.getSuperclass());
            }
            return pooled;
        }
        return null;
    }

    /**
     * Tag the given entity to the specific channel leniently
     */
    public Persistor<T> tag(Tag.Channel channel, T entity) {
        this.tag(channel, entity, false);
        return this;
    }

    /**
     * Tag the given entity to the specific channel
     *
     * @param channel   the specific channel
     * @param entity    the given entity
     * @param exclusive would not allow any other entity with the same key when
     *                  [ exclusive ] is true
     */
    @SuppressWarnings("UnusedReturnValue")
    public Persistor<T> tag(Tag.Channel channel, T entity, boolean exclusive) {
        if (entity == null)
            throw new IllegalArgumentException("The [ entity ] should not be null");

        String key = entity.key();
        if (!entities.get().containsKey(key)) {
            tagged.get().add(key);
        } else if (exclusive) {
            throw new Tag.ChannelConflictingException(key, stat(entity));
        }
        entities.get().put(key, new Tag<>(channel, entity));
        return this;
    }

    /**
     * get the status of the given entity
     */
    public Tag.Channel stat(T entity) {
        if (entity == null) return null;
        Tag<T> tag = entities.get().get(entity.key());
        if (tag != null)
            return tag.chan;
        return null;
    }

    /**
     * remove a tagged entities
     *
     * @param keys the keys of entities to be removed
     */
    public Persistor<T> untag(String... keys) {
        for (String key : keys) {
            tagged.get().remove(key);
            entities.get().remove(key);
        }
        return this;
    }

    /**
     * commit all tagged entities by using a given consumer
     *
     * @param consumer the consumer who commits entities
     */
    public void commit(BiConsumer<Tag.Channel, T> consumer) {
        if (consumer == null)
            throw new RuntimeException(
                    "Expecting a valid consumer yet got null");
        // before the entire committing process
        rough.fire(State.FORE, null);
        try {
            tagged.get().forEach(k -> {
                Tag<T> tag = entities.get().get(k);
                try {
                    // before this tag's committing
                    fine.fire(State.FORE, tag);
                    consumer.accept(tag.chan, tag.entity);
                    // after this tag's committing
                    fine.fire(State.AFT, tag);
                } catch (RuntimeException e) {
                    // caught an exception
                    except.fire(this, tag.new Ex(e));
                    throw e;
                }
            });
            // after the entire committing process
            rough.fire(State.AFT, null);
        } finally {
            this.clear();
        }
    }

    /**
     * commit all tagged entities by using a specific selector
     */
    @SuppressWarnings("unchecked")
    public final void commit(Selector<T> selector) {
        Selector<T> sel = selector == null ? Selector.ZERO_VALUE : selector;
        this.commit((chan, entity) -> {
            Repo<T> repo = sel.select(entity);
            if (repo == null) return;
            switch (chan) {
                case CREATE:
                    repo.create(entity);
                    break;
                case UPDATE:
                    repo.update(entity);
                    break;
                case DELETE:
                    repo.delete(entity);
                    break;
                default:
                    break;
            }
        });
    }

    /**
     * commit all tagged entities by using all the given repositories
     *
     * @param repos repositories
     */
    @SafeVarargs
    public final void commit(Repo<? extends T>... repos) {
        this.commit(new ReposSelector<>(repos));
    }

    public void clear() {
        tagged.get().clear();
        entities.get().clear();
    }

    /**
     * enclose a given procedure using the given repos, the tagged entities will be all
     * committed after the procedure is called, or depend on the Canceller if the given
     * proc is null
     */
    public final Stub enclose(Consumer<Stub> proc, Selector<T> selector) {
        Stub stub = new Stub(selector);
        if (proc != null) {
            proc.accept(stub);
            try {
                stub.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return stub;
    }


    /**
     * Call the consumer on the specific state of every tag being
     * processed
     */
    public Persistor<T> on(State state, Consumer<Tag<T>> then) {
        fine.on(state, then);
        return this;
    }

    /**
     * Call the consumer on the specific state of a commit process
     */
    public Persistor<T> on(State state, Runnable then) {
        rough.on(state, t -> then.run());
        return this;
    }

    /**
     * Call the consumer when an exception is caught
     */
    public Persistor<T> except(Consumer<Tag<T>.Ex> then) {
        except.on(this, then);
        return this;
    }

    public class Stub implements AutoCloseable {
        private Selector<T> using;

        private Stub(Selector<T> selector) {
            this.using = selector;
        }

        @Override
        public void close() {
            if (using != null) {
                Persistor.this.commit(using);
                using = null;
            } else {
                Persistor.this.clear();
            }
        }

        public void finish() {
            try {
                this.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            this.using = null;
        }
    }

    public enum State {

        FORE, AFT
    }
}