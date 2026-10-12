package com.enochmods.hunterseye;

import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** The server is the authority; clients submit actions, never targets or coordinates. */
public final class HunterEyeNetworking {
    private HunterEyeNetworking() {}
    public enum Action { TOGGLE_TRACKING, TOGGLE_NORMAL, TOGGLE_FULL }

    public record CommandPayload(Action action) implements CustomPacketPayload {
        public static final Type<CommandPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hunterseye", "command"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CommandPayload> CODEC = StreamCodec.ofMember(
                (p,b) -> b.writeEnum(p.action), b -> new CommandPayload(b.readEnum(Action.class)));
        @Override public Type<CommandPayload> type() { return TYPE; }
    }
    public record StatePayload(boolean hasEye, boolean tracking, ConcealmentMode mode, String status,
            boolean targetOnline, UUID targetId, String targetName, String dimension,
            double x, double y, double z, double distance) implements CustomPacketPayload {
        public static final Type<StatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hunterseye", "state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC = StreamCodec.ofMember(
                StatePayload::write, StatePayload::read);
        private void write(RegistryFriendlyByteBuf b) {
            b.writeBoolean(hasEye); b.writeBoolean(tracking); b.writeEnum(mode); b.writeUtf(status, 256);
            b.writeBoolean(targetOnline); b.writeBoolean(targetId != null);
            if (targetId != null) b.writeUUID(targetId);
            b.writeUtf(targetName, 64); b.writeUtf(dimension, 256);
            b.writeDouble(x); b.writeDouble(y); b.writeDouble(z); b.writeDouble(distance);
        }
        private static StatePayload read(RegistryFriendlyByteBuf b) {
            boolean eye=b.readBoolean(), tracking=b.readBoolean(); ConcealmentMode mode=b.readEnum(ConcealmentMode.class);
            String status=b.readUtf(256); boolean online=b.readBoolean(); UUID target=b.readBoolean()?b.readUUID():null;
            return new StatePayload(eye,tracking,mode,status,online,target,b.readUtf(64),b.readUtf(256),
                    b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble());
        }
        @Override public Type<StatePayload> type() { return TYPE; }
    }
    public record ConcealmentPayload(UUID playerId, ConcealmentMode mode) implements CustomPacketPayload {
        public static final Type<ConcealmentPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hunterseye", "concealment"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ConcealmentPayload> CODEC = StreamCodec.ofMember(
                (p,b) -> { b.writeUUID(p.playerId); b.writeEnum(p.mode); },
                b -> new ConcealmentPayload(b.readUUID(),b.readEnum(ConcealmentMode.class)));
        @Override public Type<ConcealmentPayload> type() { return TYPE; }
    }
    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(CommandPayload.TYPE, CommandPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StatePayload.TYPE, StatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConcealmentPayload.TYPE, ConcealmentPayload.CODEC);
    }
}
