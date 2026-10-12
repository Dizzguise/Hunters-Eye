package com.enochmods.eyevisuals.protocol;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Optional server-to-owner protocol. Keep this source identical in both jars. */
public record OwnerFullPayload(boolean full) implements CustomPacketPayload {
    public static final Type<OwnerFullPayload> TYPE = new Type<>(net.minecraft.resources.Identifier.parse("hunterseye:owner_full_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OwnerFullPayload> CODEC =
            CustomPacketPayload.codec((payload, buffer) -> buffer.writeBoolean(payload.full()),
                    buffer -> new OwnerFullPayload(buffer.readBoolean()));

    @Override public Type<OwnerFullPayload> type() { return TYPE; }
}
