package com.sinlo.core.timewheel;

import com.sinlo.core.timewheel.core.Ticker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Systicker the ticker using FixedThreadPool
 *
 * @author sinlo
 */
public class Systicker extends Ticker {
    private final ExecutorService x;

    public Systicker(String key) {
        this(key, 1);
    }

    public Systicker(String key, long tick) {
        this(key, tick, 20);
    }

    public Systicker(String key, long tick, int scale) {
        this(key, tick, scale, Ticker.nanoedMillis());
    }

    public Systicker(String key, long tick, int scale, long start) {
        super(tick, scale, start);
        this.x = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r, "sys.ticker.executor:".concat(key));
            t.setDaemon(false);
            return t;
        });
    }

    @Override
    public ExecutorService executor() {
        return x;
    }
}
