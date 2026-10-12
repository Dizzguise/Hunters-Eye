package com.enochmods.hunterseye;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A vanilla wire box sent only to its owner. Its displays never enter any world. */
public final class ServerGazeMarker {
    private static final double EDGE_THICKNESS=0.015;
    private record Marker(ServerPlayer owner,EdgeDisplay[] edges,String dimension,UUID target,int color,double range) {}
    private static final Map<UUID,Marker> markers=new HashMap<>();
    private ServerGazeMarker() {}

    public static void update(ServerPlayer owner,HunterEyeNetworking.StatePayload view,double range,int color) {
        String dimension=owner.level().dimension().identifier().toString();
        if(!owner.isAlive()||owner.isSpectator()||!view.hasEye()||!view.tracking()||!view.targetOnline()
                ||view.targetId()==null||!dimension.equals(view.dimension())||!Double.isFinite(view.distance())
                ||view.distance()<0||!Double.isFinite(range)||range<0||view.distance()>range) {
            clear(owner); return;
        }
        // UUID lookup consults loaded entities only; it does not request or load target chunks.
        Entity target=owner.level().getEntity(view.targetId());
        if(target==null||!target.isAlive()||target.isRemoved()||target.level()!=owner.level()
                ||owner.distanceToSqr(target)>range*range) {
            clear(owner); return;
        }
        AABB box=target.getBoundingBox();
        if(!validBox(box)) { clear(owner); return; }
        Marker marker=markers.get(owner.getUUID());
        if(marker!=null&&(!marker.dimension().equals(dimension)||!marker.target().equals(view.targetId())
                ||marker.color()!=color||marker.range()!=range||marker.owner()!=owner)) {
            clear(owner); marker=null;
        }
        boolean creating=marker==null;
        if(creating) {
            EdgeDisplay[] edges=new EdgeDisplay[12];
            for(int i=0;i<edges.length;i++) edges[i]=new EdgeDisplay(owner,color,range);
            marker=new Marker(owner,edges,dimension,view.targetId(),color,range);
            markers.put(owner.getUUID(),marker);
        }
        updateEdges(marker.edges(),box);
        for(EdgeDisplay edge:marker.edges()) {
            if(creating) {
                owner.connection.send(new ClientboundAddEntityPacket(edge.getId(),edge.getUUID(),edge.getX(),edge.getY(),edge.getZ(),
                        0,0,EntityTypes.BLOCK_DISPLAY,0,Vec3.ZERO,0));
                var values=edge.getEntityData().getNonDefaultValues();
                if(values!=null) owner.connection.send(new ClientboundSetEntityDataPacket(edge.getId(),values));
                edge.getEntityData().packDirty();
            } else {
                var values=edge.getEntityData().packDirty();
                if(values!=null) owner.connection.send(new ClientboundSetEntityDataPacket(edge.getId(),values));
                owner.connection.send(ClientboundTeleportEntityPacket.teleport(edge.getId(),
                        PositionMoveRotation.of(edge),Set.of(),false));
            }
        }
    }

    private static boolean validBox(AABB box) {
        return Double.isFinite(box.minX)&&Double.isFinite(box.minY)&&Double.isFinite(box.minZ)
                &&Double.isFinite(box.maxX)&&Double.isFinite(box.maxY)&&Double.isFinite(box.maxZ)
                &&box.getXsize()>0&&box.getYsize()>0&&box.getZsize()>0;
    }

    private static void updateEdges(EdgeDisplay[] edges,AABB box) {
        double x=box.getXsize(),y=box.getYsize(),z=box.getZsize(),half=EDGE_THICKNESS/2;
        double originX=(box.minX+box.maxX)/2,originY=box.minY-half,originZ=(box.minZ+box.maxZ)/2;
        int index=0;
        // Four rails along each axis. Their centers follow the twelve actual AABB edges.
        for(int a=0;a<2;a++) for(int b=0;b<2;b++)
            edges[index++].shape(originX,originY,originZ,-x/2-half,a*y,-z/2-half+b*z,
                    x+EDGE_THICKNESS,EDGE_THICKNESS,EDGE_THICKNESS,x,y,z);
        for(int a=0;a<2;a++) for(int b=0;b<2;b++)
            edges[index++].shape(originX,originY,originZ,-x/2-half+a*x,0,-z/2-half+b*z,
                    EDGE_THICKNESS,y+EDGE_THICKNESS,EDGE_THICKNESS,x,y,z);
        for(int a=0;a<2;a++) for(int b=0;b<2;b++)
            edges[index++].shape(originX,originY,originZ,-x/2-half+a*x,b*y,-z/2-half,
                    EDGE_THICKNESS,EDGE_THICKNESS,z+EDGE_THICKNESS,x,y,z);
    }

    public static void clear(ServerPlayer owner) {
        Marker old=markers.remove(owner.getUUID());
        if(old!=null) remove(old);
    }
    private static void remove(Marker marker) {
        int[] ids=new int[marker.edges().length];
        for(int i=0;i<ids.length;i++) ids[i]=marker.edges()[i].getId();
        marker.owner().connection.send(new ClientboundRemoveEntitiesPacket(ids));
    }
    public static void reset() {
        for(Marker marker:markers.values()) remove(marker);
        markers.clear();
    }
    /** Diagnostic IDs only; absent from all authoritative worlds. */
    public static int markerId(UUID owner) { var marker=markers.get(owner); return marker==null?-1:marker.edges()[0].getId(); }
    public static int[] markerIds(UUID owner) {
        var marker=markers.get(owner);
        if(marker==null) return new int[0];
        int[] ids=new int[marker.edges().length];
        for(int i=0;i<ids.length;i++) ids[i]=marker.edges()[i].getId();
        return ids;
    }

    /** Uses vanilla display data loading to configure its private setters without reflection or a client mixin. */
    private static final class EdgeDisplay extends Display.BlockDisplay {
        private final CompoundTag settings=new CompoundTag();
        private double previousX=-1,previousY=-1,previousZ=-1;
        EdgeDisplay(ServerPlayer owner,int color,double range) {
            super(EntityTypes.BLOCK_DISPLAY,owner.level());
            setNoGravity(true); setSilent(true); setInvulnerable(true); setGlowingTag(true);
            // Displays are inherently non-pickable and non-colliding on the vanilla client.
            CompoundTag block=new CompoundTag(); block.putString("Name","minecraft:white_concrete");
            settings.put("block_state",block);
            settings.putInt("glow_color_override",color&0xffffff);
            settings.putFloat("shadow_radius",0); settings.putFloat("shadow_strength",0);
            // Leave render-distance headroom; the authoritative range gate above controls visibility.
            settings.putFloat("view_range",(float)Math.max(1,(range+16)/16));
            settings.putInt("interpolation_duration",0); settings.putInt("teleport_duration",0);
        }
        void shape(double x,double y,double z,double tx,double ty,double tz,
                double sx,double sy,double sz,double width,double height,double depth) {
            setPos(x,y,z);
            // Translation and scale change only when the target's current pose/size changes.
            if(width==previousX&&height==previousY&&depth==previousZ) return;
            CompoundTag transformation=new CompoundTag();
            transformation.put("translation",floats(tx,ty,tz));
            transformation.put("scale",floats(sx,sy,sz));
            transformation.put("left_rotation",floats(0,0,0,1));
            transformation.put("right_rotation",floats(0,0,0,1));
            settings.put("transformation",transformation);
            // Vanilla culling is centered horizontally and extends upward from the display origin.
            settings.putFloat("width",(float)(Math.max(width,depth)+EDGE_THICKNESS));
            settings.putFloat("height",(float)(height+EDGE_THICKNESS));
            readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,registryAccess(),settings));
            previousX=width; previousY=height; previousZ=depth;
        }
        private static ListTag floats(double... values) {
            ListTag list=new ListTag();
            for(double value:values) list.add(FloatTag.valueOf((float)value));
            return list;
        }
    }
}
