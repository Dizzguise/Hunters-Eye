package com.enochmods.hunterseye.client;

import com.enochmods.hunterseye.ConcealmentMode;
import com.enochmods.hunterseye.HunterEyeNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Client presentation only. All ability requests are validated by the server. */
public final class HuntersEyeClient implements ClientModInitializer {
    private static final Map<UUID, ConcealmentMode> MODES = new HashMap<>();
    private static HunterEyeNetworking.StatePayload state;
    private static long receivedAt;
    private static Entity trackedEntity;
    private static KeyMapping gaze, normal, full;

    @Override public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("hunterseye", "powers"));
        gaze = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hunterseye.gaze", GLFW.GLFW_KEY_J, category));
        normal = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hunterseye.normal", GLFW.GLFW_KEY_K, category));
        full = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.hunterseye.full", GLFW.GLFW_KEY_L, category));
        ClientPlayNetworking.registerGlobalReceiver(HunterEyeNetworking.StatePayload.TYPE, (payload, context) -> {
            state = payload;
            receivedAt = System.nanoTime();
            trackedEntity = null;
            var level = Minecraft.getInstance().level;
            if (level != null && payload.targetId() != null && payload.targetOnline()) {
                for (Entity entity : level.entitiesForRendering()) {
                    if (payload.targetId().equals(entity.getUUID())) { trackedEntity = entity; break; }
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(HunterEyeNetworking.ConcealmentPayload.TYPE, (payload, context) -> {
            if (payload.mode() == ConcealmentMode.NONE) MODES.remove(payload.playerId());
            else MODES.put(payload.playerId(), payload.mode());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> clear());
        // Claim ability presses before vanilla opens a screen (L normally opens Advancements).
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            consumeAbility(client, gaze, HunterEyeNetworking.Action.TOGGLE_TRACKING);
            consumeAbility(client, normal, HunterEyeNetworking.Action.TOGGLE_NORMAL);
            consumeAbility(client, full, HunterEyeNetworking.Action.TOGGLE_FULL);
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, Identifier.fromNamespaceAndPath("hunterseye", "gaze"), HuntersEyeClient::drawHud);
    }

    private static void clear() { MODES.clear(); state = null; receivedAt = 0; trackedEntity = null; }
    public static ConcealmentMode mode(UUID player) { return MODES.getOrDefault(player, ConcealmentMode.NONE); }
    public static boolean fullyHidden(Entity entity) { return mode(entity.getUUID()) == ConcealmentMode.FULL; }
    private static void consumeAbility(Minecraft client, KeyMapping binding, HunterEyeNetworking.Action action) {
        while (binding.consumeClick()) {
            if (client.player == null || client.gui.screen() != null || state == null || !state.hasEye()
                    || !ClientPlayNetworking.canSend(HunterEyeNetworking.CommandPayload.TYPE)) continue;
            ClientPlayNetworking.send(new HunterEyeNetworking.CommandPayload(action));
            for (KeyMapping other : client.options.keyMappings) {
                if (other != gaze && other != normal && other != full && other.same(binding)) {
                    while (other.consumeClick()) { /* Active Eye owns this press. */ }
                    other.setDown(false);
                }
            }
        }
    }

    private static void drawHud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        var current = state;
        if (client.player == null || client.level == null || current == null || !current.hasEye()) return;
        boolean fresh = System.nanoTime() - receivedAt < 2_000_000_000L;
        String power = switch (current.mode()) { case NONE -> "Visible"; case NORMAL -> "Normal"; case FULL -> "Full"; };
        String header = "Hunter's Eye  |  Phantom Hunt: " + power;
        String target = "Hunter's Gaze: " + (current.tracking() ? current.targetName().isBlank() ? "Unbound" : current.targetName() : "Off");
        String details = current.status();
        if (current.tracking() && current.targetOnline() && fresh) {
            details = String.format(Locale.ROOT, "XYZ %.1f / %.1f / %.1f", current.x(), current.y(), current.z());
        } else if (!fresh) details = "Waiting for server";
        String dimension = current.tracking() && current.targetOnline() && fresh
                ? current.dimension() + (current.distance() >= 0 ? String.format(Locale.ROOT, "  |  %.1f blocks", current.distance()) : "  |  Other dimension") : "";
        int width = Math.max(client.font.width(header), Math.max(client.font.width(target), Math.max(client.font.width(details), client.font.width(dimension)))) + 16;
        width = Math.min(width, graphics.guiWidth() - 16);
        graphics.nextStratum();
        graphics.fill(6, 6, 6 + width, dimension.isEmpty() ? 48 : 60, 0xBA160E22);
        graphics.fill(6, 6, 8, dimension.isEmpty() ? 48 : 60, 0xFFD8B55E);
        graphics.text(client.font, header, 13, 12, 0xFFE4C778);
        graphics.text(client.font, target, 13, 24, 0xFFD3AAFF);
        graphics.text(client.font, details, 13, 36, 0xFFF2EBFF);
        if (!dimension.isEmpty()) graphics.text(client.font, dimension, 13, 48, 0xFFC0B3D0);
        if (fresh && current.tracking() && current.targetOnline()) drawMarker(client, graphics, delta, current);
    }

    /** A projected world-sized outline stays private and needs no remote chunk/entity loading. */
    private static void drawMarker(Minecraft client, GuiGraphicsExtractor graphics, DeltaTracker delta, HunterEyeNetworking.StatePayload s) {
        if (!client.level.dimension().identifier().toString().equals(s.dimension()) || s.targetId() == null) return;
        Entity target = trackedEntity;
        if (target != null && (target.isRemoved() || target.level() != client.level
                || !s.targetId().equals(target.getUUID()))) target = null;
        Vec3 feet = target != null ? target.getPosition(delta.getGameTimeDeltaPartialTick(false)) : new Vec3(s.x(), s.y(), s.z());
        if (client.player.position().distanceToSqr(feet) > 64 * 64 || s.targetId().equals(client.player.getUUID())) return;
        double halfWidth = target == null ? 0.3 : target.getBbWidth() / 2.0;
        double height = target == null ? 1.8 : target.getBbHeight();
        var camera = client.gameRenderer.mainCamera();
        var view = camera.forwardVector();
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 8; i++) {
            Vec3 corner = feet.add((i & 1) == 0 ? -halfWidth : halfWidth, (i & 2) == 0 ? 0 : height, (i & 4) == 0 ? -halfWidth : halfWidth);
            Vec3 offset = corner.subtract(camera.position());
            // Reject near-plane intersections rather than drawing mirrored or enormous boxes behind the camera.
            if (offset.x * view.x() + offset.y * view.y() + offset.z * view.z() <= 0.05) return;
            Vec3 ndc = client.gameRenderer.projectPointToScreen(corner);
            double x = (ndc.x + 1) * 0.5 * graphics.guiWidth();
            double y = (1 - ndc.y) * 0.5 * graphics.guiHeight();
            if (!Double.isFinite(x) || !Double.isFinite(y)) return;
            minX = Math.min(minX, x); minY = Math.min(minY, y); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
        }
        if (maxX < 0 || minX > graphics.guiWidth() || maxY < 0 || minY > graphics.guiHeight()) return;
        int x = (int) Math.max(-2, minX), y = (int) Math.max(-2, minY);
        int w = (int) Math.min(graphics.guiWidth() + 2, maxX) - x, h = (int) Math.min(graphics.guiHeight() + 2, maxY) - y;
        graphics.outline(x - 1, y - 1, w + 2, h + 2, 0xDD271139);
        graphics.outline(x, y, Math.max(2, w), Math.max(2, h), 0xFFE1AF63);
        graphics.text(client.font, s.targetName(), Math.max(2, x), Math.max(2, y - 11), 0xFFD3AAFF);
    }
}
