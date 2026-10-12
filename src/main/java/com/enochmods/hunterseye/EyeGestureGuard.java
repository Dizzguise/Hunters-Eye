package com.enochmods.hunterseye;

/** A click sends several packets; held mining sends repeated swings without a release packet. */
public final class EyeGestureGuard {
    private static final long QUIET_TICKS=5;
    private long lastInput=Long.MIN_VALUE;
    private long suppressThrough=Long.MIN_VALUE;

    public boolean accept(long tick) {
        boolean quiet=lastInput==Long.MIN_VALUE||tick-lastInput>=QUIET_TICKS;
        lastInput=tick;
        return tick>suppressThrough&&quiet;
    }
    public void suppressUse(long tick) {
        suppressThrough=tick+2;
        lastInput=tick;
    }
    public void continuation(long tick) { lastInput=tick; }
}
