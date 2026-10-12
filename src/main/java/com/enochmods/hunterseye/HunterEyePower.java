package com.enochmods.hunterseye;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** All mutations and snapshots run on the logical server thread. */
public final class HunterEyePower {
    private static EyeSettingsStore store;
    private static final Map<UUID,ConcealmentMode> effective=new HashMap<>();
    private static int ticks;
    private HunterEyePower() {}
    public static void start(MinecraftServer server) {
        effective.clear(); ticks=0; store=new EyeSettingsStore(server.getWorldPath(LevelResource.ROOT));
    }
    public static void save() { if(store!=null) store.save(); }
    public static void stop() { save(); store=null; effective.clear(); }
    public static ConcealmentMode getMode(UUID playerId) { return effective.getOrDefault(playerId,ConcealmentMode.NONE); }
    public static boolean isInvisible(UUID playerId) { return getMode(playerId)!=ConcealmentMode.NONE; }
    private static EyePowerState state(ServerPlayer player) { return store.state(player.getUUID()); }
    private static ItemStack carriedEye(ServerPlayer player, UUID id) {
        if(id==null) return ItemStack.EMPTY;
        // Explicitly main 36 + offhand. Never recurse into shulkers, bundles or ender storage.
        for(int i=0;i<36;i++) { ItemStack stack=player.getInventory().getItem(i); if(id.equals(EyeData.id(stack))) return stack; }
        ItemStack offhand=player.getOffhandItem(); return id.equals(EyeData.id(offhand))?offhand:ItemStack.EMPTY;
    }
    private static void identifyCarried(ServerPlayer player) {
        Set<UUID> ids=new HashSet<>();
        for(int i=0;i<37;i++) {
            ItemStack stack=i==36?player.getOffhandItem():player.getInventory().getItem(i);
            if(EyeData.isEye(stack)) {
                UUID id=EyeData.ensureId(stack);
                if(!ids.add(id)) {
                    // Creative copying duplicates custom data. Never silently migrate active powers to a clone.
                    EyePowerState current=state(player);
                    if(id.equals(current.activeEye)) { current.disable(); current.activeEye=null; store.changed(); }
                    ids.add(EyeData.renewId(stack));
                }
            }
        }
    }
    private static ItemStack validate(ServerPlayer player) {
        EyePowerState state=state(player); ItemStack eye=carriedEye(player,state.activeEye);
        if((eye.isEmpty()||!player.isAlive()||player.isSpectator()) && (state.tracking||state.mode!=ConcealmentMode.NONE)) {
            state.disable(); store.changed();
        }
        setEffective(player,eye.isEmpty()||!player.isAlive()||player.isSpectator()?ConcealmentMode.NONE:state.mode);
        return eye;
    }
    private static void setEffective(ServerPlayer player, ConcealmentMode mode) {
        UUID id=player.getUUID(); ConcealmentMode before=getMode(id);
        if(mode==ConcealmentMode.NONE) effective.remove(id); else effective.put(id,mode);
        if(before!=mode) broadcast(player.level().getServer(),new HunterEyeNetworking.ConcealmentPayload(id,mode));
    }
    private static void broadcast(MinecraftServer server,HunterEyeNetworking.ConcealmentPayload payload) {
        for(ServerPlayer observer:server.getPlayerList().getPlayers()) send(observer,payload);
    }
    private static void send(ServerPlayer player,net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if(ServerPlayNetworking.canSend(player,payload.type())) ServerPlayNetworking.send(player,payload);
    }
    public static void join(ServerPlayer player) {
        if(store==null) return;
        identifyCarried(player); validate(player);
        for(var entry:effective.entrySet()) send(player,new HunterEyeNetworking.ConcealmentPayload(entry.getKey(),entry.getValue()));
        snapshot(player);
    }
    public static void disconnect(ServerPlayer player) {
        // Saved preferences survive disconnect; observers immediately lose the disconnected entity's mode.
        effective.remove(player.getUUID());
        broadcast(player.level().getServer(),new HunterEyeNetworking.ConcealmentPayload(player.getUUID(),ConcealmentMode.NONE)); save();
    }
    public static void death(ServerPlayer player) {
        if(store==null) return; state(player).disable(); store.changed(); setEffective(player,ConcealmentMode.NONE); snapshot(player);
    }
    public static void tick(MinecraftServer server) {
        if(store==null) return;
        ticks++;
        for(ServerPlayer player:server.getPlayerList().getPlayers()) {
            identifyCarried(player); validate(player);
            if(ticks%4==0) snapshot(player);
        }
        if(ticks%100==0) save();
    }
    public static void action(ServerPlayer player,HunterEyeNetworking.Action action) {
        if(store==null) return;
        ItemStack eye=validate(player);
        if(eye.isEmpty()||!player.isAlive()||player.isSpectator()) { message(player,"Use a carried Hunter's Eye to select it first."); snapshot(player); return; }
        EyePowerState state=state(player);
        if(action==HunterEyeNetworking.Action.TOGGLE_TRACKING && !state.tracking && EyeData.target(eye)==null) {
            message(player,"This eye is unbound. Sneak + use while aiming at a visible player or mob."); return;
        }
        state.toggle(action); store.changed(); validate(player); snapshot(player);
    }
    public static void use(ServerPlayer player,ItemStack eye) {
        if(store==null||!EyeData.isEye(eye)||!player.isAlive()||player.isSpectator()) return;
        identifyCarried(player);
        UUID eyeId=EyeData.ensureId(eye); EyePowerState state=state(player);
        if(!eyeId.equals(state.activeEye)) { state.disable(); state.activeEye=eyeId; store.changed(); }
        if(player.isShiftKeyDown()) {
            LivingEntity target=visibleTarget(player);
            if(target==null) message(player,"No visible player or mob in your gaze within 32 blocks.");
            else { EyeData.bind(eye,target.getUUID(),target.getName().getString(),target instanceof Player?EyeData.TargetKind.PLAYER:EyeData.TargetKind.MOB); message(player,"Hunter's Eye bound to "+target.getName().getString()+"."); }
        } else message(player,"Hunter's Eye selected. Target: "+(EyeData.target(eye)==null?"unbound":EyeData.targetName(eye))
                +" | Gaze: "+(state.tracking?"on":"off")+" | Phantom Hunt: "+state.mode.name().toLowerCase());
        validate(player); snapshot(player);
    }
    private static void message(ServerPlayer player,String text) { player.sendSystemMessage(Component.literal(text),true); }
    private static LivingEntity visibleTarget(ServerPlayer owner) {
        Vec3 from=owner.getEyePosition(),to=from.add(owner.getViewVector(1).scale(32));
        var block=owner.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));
        double nearest=block.getType()==HitResult.Type.MISS?32*32:from.distanceToSqr(block.getLocation());
        LivingEntity target=null;
        for(Entity entity:owner.level().getEntities(owner,owner.getBoundingBox().expandTowards(to.subtract(from)).inflate(1),
                candidate->candidate instanceof Player||candidate instanceof Mob)) {
            LivingEntity candidate=(LivingEntity)entity;
            if(candidate==owner||!candidate.isAlive()||candidate.isSpectator()||getMode(candidate.getUUID())==ConcealmentMode.FULL||candidate.isInvisibleTo(owner)) continue;
            var hit=candidate.getBoundingBox().inflate(0.1).clip(from,to);
            if(hit.isEmpty()) continue;
            double distance=from.distanceToSqr(hit.get());
            if(distance<nearest && owner.hasLineOfSight(candidate)) { nearest=distance; target=candidate; }
        }
        return target;
    }
    public static void offensiveAction(Entity attacker) {
        if(!(attacker instanceof ServerPlayer player)||store==null) return;
        validate(player);
        if(state(player).mode==ConcealmentMode.FULL) {
            state(player).mode=ConcealmentMode.NONE; store.changed(); setEffective(player,ConcealmentMode.NONE);
            message(player,"Phantom Hunt: Full ended after an offensive action."); snapshot(player);
        }
    }
    public static void snapshot(ServerPlayer player) {
        if(store==null) return;
        validate(player);
        send(player,snapshotState(player));
    }
    /** Read-only view of current server state, including only entities already loaded. */
    public static HunterEyeNetworking.StatePayload snapshotState(ServerPlayer player) {
        EyePowerState state=store==null?new EyePowerState():store.states.getOrDefault(player.getUUID(),new EyePowerState());
        ItemStack eye=carriedEye(player,state.activeEye);
        boolean enabled=!eye.isEmpty()&&player.isAlive()&&!player.isSpectator();
        boolean tracking=enabled&&state.tracking;
        UUID targetId=eye.isEmpty()?null:EyeData.target(eye); String name=eye.isEmpty()?"":EyeData.targetName(eye);
        Entity target=null;
        if(targetId!=null) {
            for(var level:player.level().getServer().getAllLevels()) {
                Entity loaded=level.getEntity(targetId);
                if(loaded instanceof Player||loaded instanceof Mob) { target=loaded; break; }
            }
        }
        if(target!=null) name=target.getName().getString();
        boolean live=tracking&&target!=null&&target.isAlive();
        String unavailable=EyeData.targetKind(eye)==EyeData.TargetKind.MOB?"Target unavailable / unloaded":"Target offline";
        String status=eye.isEmpty()?"No active eye":targetId==null?"Unbound":!tracking?"Tracking off":target==null?unavailable:!target.isAlive()?"Target dead":target.level()!=player.level()?"Target in another dimension":"Tracking";
        String dimension=live?target.level().dimension().identifier().toString():"";
        double distance=live&&target.level()==player.level()?player.distanceTo(target):-1;
        return new HunterEyeNetworking.StatePayload(!eye.isEmpty(),tracking,enabled?state.mode:ConcealmentMode.NONE,status,live,targetId,networkName(name),dimension,
                live?target.getX():0,live?target.getY():0,live?target.getZ():0,distance);
    }
    private static String networkName(String name) {
        // The state codec permits 64 UTF-16 characters. Never split a surrogate pair.
        if(name.length()<=64) return name;
        int end=64;
        if(Character.isHighSurrogate(name.charAt(end-1))) end--;
        return name.substring(0,end);
    }
}
