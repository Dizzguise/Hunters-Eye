package com.enochmods.hunterseye;

import java.util.UUID;

/** Local compatibility DTOs for verification; no custom packets are registered or sent. */
public final class HunterEyeNetworking {
    private HunterEyeNetworking() {}
    public enum Action { TOGGLE_TRACKING, TOGGLE_NORMAL, TOGGLE_FULL }
    public record StatePayload(boolean hasEye, boolean tracking, ConcealmentMode mode, String status,
            boolean targetOnline, UUID targetId, String targetName, String dimension,
            double x, double y, double z, double distance) {}
}
