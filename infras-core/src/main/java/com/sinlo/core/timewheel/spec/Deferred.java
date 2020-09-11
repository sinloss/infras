package com.sinlo.core.timewheel.spec;

/**
 * Defered the deferred runnable
 *
 * @author sinlo
 */
public abstract class Deferred implements Cylinder.EntryAware<Deferred>, Runnable {

    protected final long defer;

    private Cylinder.Entry<Deferred> entry;

    public Deferred(long defer) {
        this.defer = defer;
    }

    public synchronized void cancel() {
        if (entry != null) {
            entry.remove();
            entry = null;
        }
    }

    public synchronized void setEntry(Cylinder.Entry<Deferred> entry) {
        if (this.entry != null && !this.entry.equals(entry)) {
            this.entry.remove();
        }
        this.entry = entry;
    }

    public Cylinder.Entry<Deferred> getEntry() {
        return entry;
    }
}
