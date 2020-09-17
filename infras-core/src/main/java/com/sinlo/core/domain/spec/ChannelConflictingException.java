package com.sinlo.core.domain.spec;

/**
 * @author sinlo
 */
public class ChannelConflictingException extends IllegalStateException {

    public ChannelConflictingException(String key, Channel channel) {
        super("Cannot tag the current object [ "
                .concat(key)
                .concat(" ], because it has already been tagged ")
                .concat(channel == null ? "" : "as " + channel.toString()));
    }
}
