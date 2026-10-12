package com.enochmods.hunterseye;

import java.util.UUID;

/** Independent tracking and concealment, with exactly one explicitly selected eye. */
public final class EyePowerState {
    public UUID activeEye;
    public boolean tracking;
    public ConcealmentMode mode = ConcealmentMode.NONE;
    public void disable() { tracking=false; mode=ConcealmentMode.NONE; }
    public void toggle(HunterEyeNetworking.Action action) {
        switch(action) {
            case TOGGLE_TRACKING -> tracking=!tracking;
            case TOGGLE_NORMAL -> mode=mode==ConcealmentMode.NORMAL?ConcealmentMode.NONE:ConcealmentMode.NORMAL;
            case TOGGLE_FULL -> mode=mode==ConcealmentMode.FULL?ConcealmentMode.NONE:ConcealmentMode.FULL;
        }
    }
}
