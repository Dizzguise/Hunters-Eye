package com.enochmods.hunterseye;

/** Small pure policy surface shared by runtime checks and regression tests. */
public final class EyePolicy {
    private EyePolicy() {}
    public static boolean isFullNonCombat(ConcealmentMode mode,boolean eligibleEye) { return eligibleEye&&mode==ConcealmentMode.FULL; }
    public static boolean canTrack(boolean sameDimension,double coordinateDistance,EyeConfig config) {
        return sameDimension?coordinateDistance<=config.trackingRange():config.crossDimension();
    }
}
