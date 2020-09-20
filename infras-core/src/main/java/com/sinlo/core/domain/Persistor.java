package com.sinlo.core.domain;

import com.sinlo.core.common.Eventer;
import com.sinlo.core.domain.spec.*;
import com.sinlo.core.common.wraparound.SureThreadLocal;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Persistor the persistor who persists tagged entitys, this is specifically designed to
 * tackle the entity persisting problems inside of aggregate roots
 *
 * @author sinlo
 */
public class Persistor<T extends Entity> {

    private static final Map<String, Persistor<?>> instances = new HashMap<>();

    private final SureThreadLocal<List<String>> tagged = SureThreadLocal.of(LinkedList::new);
    private final SureThreadLocal<Map<String, Tag<T>>> entities = SureThreadLocal.of(HashMap::new);

    private final Eventer<State, Tag<T>> fine = new Eventer<>();
    private final Eventer<State, Void> rough = new Eventer<>();
    private final Eventer<Persistor<T>, Tag<T>.Ex> except = new Eventer<>();

    private Persistor() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> Persistor<T> of(Class<T> clz) {
        Persistor<T> inst = (Persistor<T>) instances.get(clz.getName());
        if (inst == null) instances.put(clz.getName(), inst = new Persistor<>());
        return inst;
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
        this.commit(new RepositoriesSelector<>(repos));
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
        public void close() throws Exception {
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