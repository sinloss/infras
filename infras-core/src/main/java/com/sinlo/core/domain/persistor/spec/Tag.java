package com.sinlo.core.domain.persistor.spec;

/**
 * Tag the tagged tag
 *
 * @author sinlo
 */
public class Tag<T extends Entity> {
    public final Channel chan;
    public final T entity;

    public Tag(Channel chan, T entity) {
        this.chan = chan;
        this.entity = entity;
    }

    public class Ex extends Tag<T> {

        public final Exception ex;

        public Ex(Exception ex) {
            super(Tag.this.chan, Tag.this.entity);
            this.ex = ex;
        }
    }

    /**
     * Tagging channel
     */
    public enum Channel {
        CREATE, UPDATE, DELETE
    }

    /**
     * Channel conflicting exception
     */
    public static class ChannelConflictingException extends IllegalStateException {

        public ChannelConflictingException(String key, Channel channel) {
            super("Cannot tag the current object [ "
                    .concat(key)
                    .concat(" ], because it has already been tagged ")
                    .concat(channel == null ? "" : "as " + channel.toString()));
        }
    }
}
