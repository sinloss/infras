package com.sinlo.core.common.functional;

import com.sinlo.core.common.util.Try;

/**
 * The impatient runnable who throws the specific exception ASAP
 *
 * @author sinlo
 */
@FunctionalInterface
public interface ImpatientRunnable<E extends Throwable> extends Runnable {

    void runs() throws E;

    @Override
    default void run() {
        try {
            runs();
        } catch (Throwable e) {
            Try.toss(e);
        }
    }
}
