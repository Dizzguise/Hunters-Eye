package com.enochmods.hunterseye;

import java.util.UUID;

/** Independent tracking and concealment, with exactly one explicitly selected eye. */
public final class EyePowerState {
    public UUID activeEye;
    public boolean tracking;
    public ConcealmentMode mode = ConcealmentMode.NONE;
    /** Real-time, player-owned deadlines; losing or switching an Eye never clears cooldowns. */
    public long normalCooldownUntil;
    public long fullCooldownUntil;
    /** Absolute effect deadline, shared by the mutually exclusive concealment modes. */
    public long modeUntil;
    public void disable() { tracking=false; mode=ConcealmentMode.NONE; modeUntil=0; }
    /** Legacy active saves receive one bounded timer; later restores preserve its deadline. */
    public boolean restoreTimer(long nowMillis,EyeConfig config) {
        if(mode==ConcealmentMode.NONE) {
            if(modeUntil==0) return false;
            modeUntil=0; return true;
        }
        if(modeUntil==0) {
            modeUntil=deadline(nowMillis,mode==ConcealmentMode.NORMAL?config.normalDuration():config.fullDuration());
            return true;
        }
        return expire(nowMillis);
    }
    /** Expiry stops concealment while preserving Gaze and activation cooldowns. */
    public boolean expire(long nowMillis) {
        if(mode==ConcealmentMode.NONE||nowMillis<modeUntil) return false;
        mode=ConcealmentMode.NONE; modeUntil=0; return true;
    }
    public boolean requestMode(ConcealmentMode requested,long nowMillis,int cooldownSeconds) {
        return requestMode(requested,nowMillis,cooldownSeconds,
                requested==ConcealmentMode.NORMAL?EyeConfig.DEFAULT_NORMAL_DURATION:EyeConfig.DEFAULT_FULL_DURATION);
    }
    public boolean requestMode(ConcealmentMode requested,long nowMillis,int cooldownSeconds,int durationSeconds) {
        if(!EyeConfig.validCooldown(cooldownSeconds)) throw new IllegalArgumentException("Cooldown must be 0..3600 seconds");
        if(!EyeConfig.validDuration(durationSeconds)) throw new IllegalArgumentException("Duration must be 1..3600 seconds");
        // Validate before same-mode idempotence: an expired mode is a fresh activation.
        expire(nowMillis);
        if(requested==mode) return true;
        if(requested==ConcealmentMode.NONE) { mode=requested; modeUntil=0; return true; }
        if(remainingSeconds(requested,nowMillis)>0) return false;
        long cooldownUntil=deadline(nowMillis,cooldownSeconds);
        if(requested==ConcealmentMode.NORMAL) normalCooldownUntil=cooldownUntil; else fullCooldownUntil=cooldownUntil;
        mode=requested; modeUntil=deadline(nowMillis,durationSeconds);
        return true;
    }
    private static long deadline(long nowMillis,int seconds) {
        long duration=seconds*1000L;
        return nowMillis>Long.MAX_VALUE-duration?Long.MAX_VALUE:nowMillis+duration;
    }
    private static long remaining(long until,long nowMillis) {
        return nowMillis>=until?0:1+(until-nowMillis-1)/1000;
    }
    public long remainingDurationSeconds(long nowMillis) {
        return mode==ConcealmentMode.NONE?0:remaining(modeUntil,nowMillis);
    }
    public long remainingSeconds(ConcealmentMode requested,long nowMillis) {
        return remaining(switch(requested) {
            case NONE -> 0;
            case NORMAL -> normalCooldownUntil;
            case FULL -> fullCooldownUntil;
        },nowMillis);
    }
    public boolean toggle(HunterEyeNetworking.Action action,long nowMillis,EyeConfig config) {
        expire(nowMillis);
        switch(action) {
            case TOGGLE_TRACKING -> { tracking=!tracking; return true; }
            case TOGGLE_NORMAL -> { return requestMode(mode==ConcealmentMode.NORMAL?ConcealmentMode.NONE:ConcealmentMode.NORMAL,nowMillis,config.normalCooldown(),config.normalDuration()); }
            case TOGGLE_FULL -> { return requestMode(mode==ConcealmentMode.FULL?ConcealmentMode.NONE:ConcealmentMode.FULL,nowMillis,config.fullCooldown(),config.fullDuration()); }
        }
        throw new IllegalArgumentException("Unknown action");
    }
}
