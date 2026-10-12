package com.enochmods.hunterseye;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

/** All mutations and local snapshots run on the logical server thread. */
public final class HunterEyePower {
    private static EyeSettingsStore store;
    private static EyeConfig config=new EyeConfig();
    private static final Map<UUID,ConcealmentMode> effective=new HashMap<>();
    private static final Set<UUID> actionbarVisible=new HashSet<>();
    private static int ticks;
    private HunterEyePower() {}
    public static EyeConfig config() { return config; }
    public static void start(MinecraftServer server) {
        effective.clear(); actionbarVisible.clear(); HunterEyeControls.clear(); ServerGazeMarker.reset(); ticks=0;
        var root=server.getWorldPath(LevelResource.ROOT);
        config=new EyeConfig(root); store=new EyeSettingsStore(root);
        store.restoreTimers(System.currentTimeMillis(),config);
    }
    public static void save() { if(store!=null) store.save(); }
    public static void stop() { save(); store=null; effective.clear(); actionbarVisible.clear(); HunterEyeControls.clear(); ServerGazeMarker.reset(); config=new EyeConfig(); }
    public static ConcealmentMode getMode(UUID playerId) { return effective.getOrDefault(playerId,ConcealmentMode.NONE); }
    public static boolean isInvisible(UUID playerId) { return getMode(playerId)!=ConcealmentMode.NONE; }
    /** Fresh, side-effect-free checks: combat hooks must not trigger metadata updates. */
    private static ItemStack eligibleActiveEye(ServerPlayer player) {
        if(store==null) return ItemStack.EMPTY;
        EyePowerState state=store.states.get(player.getUUID());
        if(state==null||state.activeEye==null) return ItemStack.EMPTY;
        ItemStack eye=carriedEye(player,state.activeEye);
        if(!usable(player,eye)) return ItemStack.EMPTY;
        int matches=0;
        for(int i=0;i<37;i++) {
            ItemStack stack=i==36?player.getOffhandItem():player.getInventory().getItem(i);
            if(state.activeEye.equals(EyeData.id(stack))&&++matches>1) return ItemStack.EMPTY;
        }
        return eye;
    }
    public static boolean isFullNonCombat(ServerPlayer player) {
        ItemStack eye=eligibleActiveEye(player);
        EyePowerState current=store==null?null:store.states.get(player.getUUID());
        return EyePolicy.isFullNonCombat(current==null||current.remainingDurationSeconds(System.currentTimeMillis())==0?ConcealmentMode.NONE:current.mode,!eye.isEmpty());
    }
    public static boolean blocksOutgoingDamage(Entity attacker) {
        return attacker instanceof ServerPlayer player&&isFullNonCombat(player);
    }
    private static EyePowerState state(ServerPlayer player) { return store.state(player.getUUID()); }
    private static ItemStack carriedEye(ServerPlayer player,UUID id) {
        if(id==null) return ItemStack.EMPTY;
        // Explicit main 36 + offhand. Never recurse into containers or ender storage.
        for(int i=0;i<36;i++) {
            ItemStack stack=player.getInventory().getItem(i);
            if(id.equals(EyeData.id(stack))) return stack;
        }
        ItemStack offhand=player.getOffhandItem();
        return id.equals(EyeData.id(offhand))?offhand:ItemStack.EMPTY;
    }
    private static void identifyCarried(ServerPlayer player) {
        Set<UUID> ids=new HashSet<>();
        for(int i=0;i<37;i++) {
            ItemStack stack=i==36?player.getOffhandItem():player.getInventory().getItem(i);
            if(EyeData.isEye(stack)) {
                UUID id=EyeData.ensureId(stack);
                if(!ids.add(id)) {
                    // Creative copies cannot silently inherit active powers.
                    EyePowerState current=state(player);
                    if(id.equals(current.activeEye)) { current.disable(); current.activeEye=null; store.changed(); }
                    ids.add(EyeData.renewId(stack));
                }
            }
        }
    }
    private static boolean usable(ServerPlayer player,ItemStack eye) {
        return !eye.isEmpty()&&player.isAlive()&&!player.isSpectator();
    }
    private static ItemStack validate(ServerPlayer player) {
        EyePowerState state=state(player); ItemStack eye=carriedEye(player,state.activeEye);
        if(state.restoreTimer(System.currentTimeMillis(),config)) store.changed();
        if(!usable(player,eye)&&(state.tracking||state.mode!=ConcealmentMode.NONE)) {
            state.disable(); store.changed();
        }
        setEffective(player,usable(player,eye)?state.mode:ConcealmentMode.NONE);
        return eye;
    }
    private static void setEffective(ServerPlayer player,ConcealmentMode mode) {
        UUID id=player.getUUID(); ConcealmentMode before=getMode(id);
        if(mode==ConcealmentMode.NONE) effective.remove(id); else effective.put(id,mode);
        if(before!=mode) ServerConcealment.changed(player);
    }
    public static void join(ServerPlayer player) {
        if(store==null) return;
        identifyCarried(player); validate(player); ServerConcealment.changed(player); snapshot(player);
    }
    /** Restore saved mode before the newly joined entity is first paired with observers. */
    public static void prepareJoin(ServerPlayer player) {
        if(store==null) return;
        identifyCarried(player); validate(player);
    }
    public static void disconnect(ServerPlayer player) {
        ServerGazeMarker.clear(player); effective.remove(player.getUUID()); actionbarVisible.remove(player.getUUID()); HunterEyeControls.forget(player); save();
    }
    public static void death(ServerPlayer player) {
        if(store==null) return;
        state(player).disable(); store.changed(); setEffective(player,ConcealmentMode.NONE); snapshot(player);
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
    private static ItemStack requireActive(ServerPlayer player) {
        if(store==null) return ItemStack.EMPTY;
        identifyCarried(player); ItemStack eye=validate(player);
        if(!usable(player,eye)) {
            message(player,"Use a carried Hunter's Eye to select it first.");
            snapshot(player); return ItemStack.EMPTY;
        }
        return eye;
    }
    public static void action(ServerPlayer player,HunterEyeNetworking.Action action) {
        ItemStack eye=requireActive(player); if(eye.isEmpty()) return;
        EyePowerState state=state(player);
        if(action==HunterEyeNetworking.Action.TOGGLE_TRACKING&&!state.tracking&&EyeData.target(eye)==null) {
            message(player,"This eye is unbound. Sneak + use while aiming at a visible player or mob."); return;
        }
        if(action==HunterEyeNetworking.Action.TOGGLE_TRACKING) {
            state.tracking=!state.tracking; store.changed(); validate(player); status(player);
        } else {
            ConcealmentMode requested=action==HunterEyeNetworking.Action.TOGGLE_NORMAL?ConcealmentMode.NORMAL:ConcealmentMode.FULL;
            transition(player,state.mode==requested?ConcealmentMode.NONE:requested);
        }
    }
    /** Check activation before selecting a new Eye, so a blocked gesture preserves current powers. */
    public static void heldAction(ServerPlayer player,HunterEyeNetworking.Action action) {
        if(store==null||!EyeData.isEye(player.getMainHandItem())) return;
        identifyCarried(player); validate(player);
        ItemStack eye=player.getMainHandItem();
        if(!usable(player,eye)) {
            validate(player); return;
        }
        if(action!=HunterEyeNetworking.Action.TOGGLE_TRACKING) {
            EyePowerState current=state(player);
            ConcealmentMode requested=action==HunterEyeNetworking.Action.TOGGLE_NORMAL?ConcealmentMode.NORMAL:ConcealmentMode.FULL;
            long now=System.currentTimeMillis();
            if(!(EyeData.id(eye).equals(current.activeEye)&&current.mode==requested)
                    &&current.remainingSeconds(requested,now)>0) {
                cooldownMessage(player,requested,now); return;
            }
        }
        select(player,EyeData.id(eye));
        action(player,action);
    }
    public static void setPhantom(ServerPlayer player,ConcealmentMode mode) {
        if(store==null) return;
        if(mode!=ConcealmentMode.NONE&&requireActive(player).isEmpty()) return;
        transition(player,mode);
    }
    private static void transition(ServerPlayer player,ConcealmentMode requested) {
        validate(player);
        long now=System.currentTimeMillis();
        int duration=requested==ConcealmentMode.NORMAL?config.normalDuration():config.fullDuration();
        int cooldown=requested==ConcealmentMode.NORMAL?config.normalCooldown():config.fullCooldown();
        EyePowerState current=state(player); ConcealmentMode before=current.mode;
        if(!current.requestMode(requested,now,cooldown,duration)) {
            // A deadline can elapse between validation and the request's timestamp.
            if(current.mode!=before) { store.changed(); validate(player); }
            cooldownMessage(player,requested,now); return;
        }
        store.changed(); validate(player); status(player);
    }
    private static void cooldownMessage(ServerPlayer player,ConcealmentMode mode,long now) {
        message(player,"Phantom Hunt: "+mode.name().toLowerCase()+" cooldown: "+state(player).remainingSeconds(mode,now)+"s remaining.");
    }
    public static void use(ServerPlayer player,ItemStack eye) {
        HunterEyeControls.use(player);
        if(store==null||!EyeData.isEye(eye)||!player.isAlive()||player.isSpectator()) return;
        identifyCarried(player);
        // Reject fabricated stack arguments on every use.
        UUID eyeId=EyeData.id(eye);
        if(eyeId==null||carriedEye(player,eyeId)!=eye) return;
        if(!usable(player,eye)) { validate(player); return; }
        select(player,eyeId);
        if(player.isShiftKeyDown()) {
            LivingEntity target=visibleTarget(player);
            if(target==null) message(player,"No visible player or mob in your gaze within "+config.bindRange()+" blocks.");
            else bind(player,eye,target);
        } else status(player);
        validate(player); player.containerMenu.broadcastChanges();
    }
    private static void select(ServerPlayer player,UUID eyeId) {
        EyePowerState state=state(player);
        if(!eyeId.equals(state.activeEye)) {
            state.disable(); state.activeEye=eyeId; store.changed(); validate(player);
        }
    }
    private static void bind(ServerPlayer player,ItemStack eye,Entity target) {
        EyeData.bind(eye,target.getUUID(),target.getName().getString(),target instanceof Player?EyeData.TargetKind.PLAYER:EyeData.TargetKind.MOB);
        message(player,"Hunter's Eye bound to "+target.getName().getString()+".");
    }
    public static void status(ServerPlayer player) {
        if(store==null) return;
        identifyCarried(player); validate(player);
        var view=snapshotState(player);
        String identity=view.hasEye()?"Hunter's Eye":"No active eye";
        long now=System.currentTimeMillis();
        message(player,identity+" | Target: "+(view.targetId()==null?"unbound":view.targetName())
                +" | Gaze: "+(view.tracking()?"on":"off")+" | "+phantomStatus(state(player),now)+" | "+view.status()
                +" | Normal cooldown: "+state(player).remainingSeconds(ConcealmentMode.NORMAL,now)+"s"
                +" | Full cooldown: "+state(player).remainingSeconds(ConcealmentMode.FULL,now)+"s");
    }
    private static void message(ServerPlayer player,String text) {
        player.sendSystemMessage(Component.literal(text).withStyle(style->style.withColor(config.color())),true);
    }
    private static LivingEntity visibleTarget(ServerPlayer owner) {
        double range=config.bindRange();
        Vec3 from=owner.getEyePosition(),to=from.add(owner.getViewVector(1).scale(range));
        var block=owner.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));
        double nearest=block.getType()==HitResult.Type.MISS?range*range:from.distanceToSqr(block.getLocation());
        LivingEntity target=null;
        for(Entity entity:owner.level().getEntities(owner,owner.getBoundingBox().expandTowards(to.subtract(from)).inflate(1),
                candidate->candidate instanceof Player||candidate instanceof Mob)) {
            LivingEntity candidate=(LivingEntity)entity;
            if(candidate==owner||!candidate.isAlive()||candidate.isSpectator()||getMode(candidate.getUUID())==ConcealmentMode.FULL||candidate.isInvisibleTo(owner)) continue;
            var hit=candidate.getBoundingBox().inflate(0.1).clip(from,to); if(hit.isEmpty()) continue;
            double distance=from.distanceToSqr(hit.get());
            if(distance<nearest&&owner.hasLineOfSight(candidate)) { nearest=distance; target=candidate; }
        }
        return target;
    }
    public static void offensiveAction(Entity attacker) {
        if(attacker instanceof ServerPlayer player&&isFullNonCombat(player))
            message(player,"Phantom Hunt: Full is noncombat. Use /hunterseye phantom off to attack.");
    }
    public static void snapshot(ServerPlayer player) {
        if(store==null) return;
        validate(player); var view=snapshotState(player);
        ServerGazeMarker.update(player,view,config.highlightRange(),config.color());
        if(view.tracking()) {
            String text=view.targetOnline()
                    ?String.format(java.util.Locale.ROOT,"%s: %.1f, %.1f, %.1f | %s%s",view.targetName(),view.x(),view.y(),view.z(),view.dimension(),
                        view.distance()<0?"":String.format(java.util.Locale.ROOT," | %.1f blocks",view.distance()))
                    :view.targetName()+": "+view.status();
            message(player,phantomStatus(state(player),System.currentTimeMillis())+" | "+text); actionbarVisible.add(player.getUUID());
        } else if(view.mode()!=ConcealmentMode.NONE) {
            message(player,phantomStatus(state(player),System.currentTimeMillis())+" | "+view.status()); actionbarVisible.add(player.getUUID());
        } else if(actionbarVisible.remove(player.getUUID())) player.sendSystemMessage(Component.empty(),true);
    }
    private static String phantomStatus(EyePowerState state,long now) {
        return "Phantom: "+state.mode.name().toLowerCase(java.util.Locale.ROOT)
                +(state.mode==ConcealmentMode.NONE?"":" ("+state.remainingDurationSeconds(now)+"s remaining)");
    }
    /** Read-only DTO contains zero coordinates and no dimension whenever tracking is restricted. */
    public static HunterEyeNetworking.StatePayload snapshotState(ServerPlayer player) {
        if(store!=null) validate(player);
        EyePowerState state=store==null?new EyePowerState():store.states.getOrDefault(player.getUUID(),new EyePowerState());
        ItemStack eye=carriedEye(player,state.activeEye);
        boolean enabled=usable(player,eye),tracking=enabled&&state.tracking;
        UUID targetId=eye.isEmpty()?null:EyeData.target(eye); String name=eye.isEmpty()?"":EyeData.targetName(eye);
        Entity target=null;
        if(tracking&&targetId!=null) {
            for(var level:player.level().getServer().getAllLevels()) {
                Entity loaded=level.getEntity(targetId);
                if(loaded instanceof Player||loaded instanceof Mob) { target=loaded; break; }
            }
        }
        boolean sameDimension=target!=null&&target.level()==player.level();
        double coordinateDistance=!sameDimension?Double.POSITIVE_INFINITY:player.position().distanceTo(target.position());
        boolean withinPolicy=target!=null&&EyePolicy.canTrack(sameDimension,coordinateDistance,config);
        boolean live=tracking&&target!=null&&target.isAlive()&&withinPolicy;
        if(live) name=target.getName().getString();
        String unavailable=EyeData.targetKind(eye)==EyeData.TargetKind.MOB?"Target unavailable / unloaded":"Target offline";
        String status=eye.isEmpty()?"No active eye":!enabled?"Eye unavailable":targetId==null?"Unbound":!tracking?"Tracking off":
                target==null?unavailable:!target.isAlive()?"Target dead":
                !sameDimension&&!config.crossDimension()?"Target in another dimension":
                !withinPolicy?"Target outside tracking range":"Tracking";
        return new HunterEyeNetworking.StatePayload(!eye.isEmpty(),tracking,enabled?state.mode:ConcealmentMode.NONE,status,live,targetId,name,
                live?target.level().dimension().identifier().toString():"",live?target.getX():0,live?target.getY():0,live?target.getZ():0,
                live&&sameDimension?coordinateDistance:-1);
    }
}
