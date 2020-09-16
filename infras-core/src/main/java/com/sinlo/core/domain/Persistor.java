package com.sinlo.core.domain;

import com.sinlo.core.common.Eventer;
import com.sinlo.core.domain.spec.*;
import com.sinlo.core.common.wraparound.SureThreadLocal;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    private final Eventer<Channel, T> eventer = new Eventer<>();

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
    public Persistor<T> tag(Channel channel, T entity) {
        this.tag(channel, entity, false);
        return this;
    }

    /**
     * Tag the given entity to the specific channel
     *
     * @param channel   the specific channel
     * @param entity    the given entity
     * @param exclusive would not allow any other entity with the same key when [ exclusive ] is true
     */
    @SuppressWarnings("UnusedReturnValue")
    public Persistor<T> tag(Channel channel, T entity, boolean exclusive) {
        if (entity == null)
            throw new IllegalArgumentException("The [ entity ] should not be null");

        String key = entity.key();
        if (!entities.get().containsKey(key)) {
            tagged.get().add(key);
        } else if (exclusive) {
            throw new ChannelConflictingException(key, stat(entity));
        }
        entities.get().put(key, new Tag<>(channel, entity));
        return this;
    }

    /**
     * get the status of the given entity
     */
    public Channel stat(T entity) {
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
    public void commit(BiConsumer<Channel, T> consumer) {
        if (consumer == null)
            throw new RuntimeException("Expecting a valid consumer yet got null");
        tagged.get().forEach(k -> {
            Tag<T> tag = entities.get().get(k);
            eventer.fire(tag.chan, tag.entity);
            consumer.accept(tag.chan, tag.entity);
        });
        this.clear();
    }

    /**
     * commit all tagged entities by using all the given repositories
     *
     * @param repos repositories
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final void commit(Repo<? extends T>... repos) {
        final Map<String, Repo<? extends T>> mapping = Arrays.stream(repos)
                .collect(Collectors.toMap(Repo::support, r -> r));
        this.commit((chan, entity) -> {
            Repo<T> repo = (Repo<T>) mapping.get(entity.getClass().getName());
            switch (chan) {
                case CREATE:
                    repo.save(entity);
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

    public void clear() {
        tagged.get().clear();
        entities.get().clear();
    }

    /**
     * enclose a given procedure using the given repos, the tagged entities will be all
     * committed after the procedure is called, or depend on the Canceller if the given
     * proc is null
     */
    @SafeVarargs
    public final Stub enclose(Consumer<Stub> proc, Repo<? extends T>... repos) {
        Stub stub = new Stub(repos);
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
     * @see Eventer#once(Object, Consumer)
     */
    public Persistor<T> once(Channel chan, Consumer<T> consumer) {
        eventer.once(chan, consumer);
        return this;
    }

    /**
     * @see Eventer#on(Object, Consumer)
     */
    public Persistor<T> on(Channel chan, Consumer<T> consumer) {
        eventer.on(chan, consumer);
        return this;
    }

    public class Stub implements AutoCloseable {
        private Repo<? extends T>[] using = null;

        @SafeVarargs
        private Stub(Repo<? extends T>... repos) {
            this.using = repos;
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

}