package com.enochmods.hunterseye;

import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Executable regressions for independent power state, safe persistence and the exact wire contract. */
public final class PowerVerification {
    private static int checks;
    public static void main(String[] args) throws Exception {
        transitions();
        Path root=Files.createTempDirectory("hunterseye-settings-test-");
        try { persistence(root); } finally {
            try(var paths=Files.walk(root)) {
                for(Path path:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
        packets();
        System.out.println("Hunter's Eye JVM verification: "+checks+" checks passed.");
    }
    private static void transitions() {
        UUID eye=UUID.randomUUID(); EyePowerState state=new EyePowerState(); state.activeEye=eye;
        check(!state.tracking&&state.mode==ConcealmentMode.NONE,"new state has all powers off");
        state.toggle(HunterEyeNetworking.Action.TOGGLE_TRACKING);
        state.toggle(HunterEyeNetworking.Action.TOGGLE_NORMAL);
        check(state.tracking&&state.mode==ConcealmentMode.NORMAL,"gaze and normal are independent");
        state.toggle(HunterEyeNetworking.Action.TOGGLE_FULL);
        check(state.tracking&&state.mode==ConcealmentMode.FULL,"full replaces normal without stopping gaze");
        state.toggle(HunterEyeNetworking.Action.TOGGLE_NORMAL);
        check(state.tracking&&state.mode==ConcealmentMode.NORMAL,"normal replaces full without stopping gaze");
        state.toggle(HunterEyeNetworking.Action.TOGGLE_NORMAL);
        check(state.tracking&&state.mode==ConcealmentMode.NONE,"normal toggles off independently");
        state.toggle(HunterEyeNetworking.Action.TOGGLE_FULL); state.toggle(HunterEyeNetworking.Action.TOGGLE_FULL);
        check(state.tracking&&state.mode==ConcealmentMode.NONE,"full toggles off independently");
        state.toggle(HunterEyeNetworking.Action.TOGGLE_FULL); state.toggle(HunterEyeNetworking.Action.TOGGLE_TRACKING);
        check(!state.tracking&&state.mode==ConcealmentMode.FULL,"turning gaze off keeps full");
        state.tracking=true; state.disable();
        check(!state.tracking&&state.mode==ConcealmentMode.NONE,"death/loss disable both powers");
        check(eye.equals(state.activeEye),"disable retains selected eye identity for explicit reactivation");
    }
    private static void persistence(Path root) throws Exception {
        UUID alice=UUID.randomUUID(),bob=UUID.randomUUID(),eye=UUID.randomUUID();
        Path world=root.resolve("world-a"); EyeSettingsStore first=new EyeSettingsStore(world);
        check(!first.state(alice).tracking&&first.state(alice).mode==ConcealmentMode.NONE,"absent settings fail closed");
        first.state(alice).activeEye=eye; first.state(alice).tracking=true; first.state(alice).mode=ConcealmentMode.FULL;
        first.changed();
        Path file=world.resolve("data/hunterseye-settings.json");
        check(Files.exists(file),"mutation immediately writes settings");
        check(!Files.exists(file.resolveSibling("hunterseye-settings.json.tmp")),"successful atomic save consumes temporary file");
        EyeSettingsStore restarted=new EyeSettingsStore(world);
        check(eye.equals(restarted.state(alice).activeEye),"restart preserves selected stack identity");
        check(restarted.state(alice).tracking&&restarted.state(alice).mode==ConcealmentMode.FULL,"restart preserves both power choices");
        check(restarted.state(bob).activeEye==null&&!restarted.state(bob).tracking,"players remain isolated");
        check(new EyeSettingsStore(root.resolve("world-b")).state(alice).activeEye==null,"worlds remain isolated");
        restarted.state(alice).disable(); restarted.changed();
        EyeSettingsStore afterDeath=new EyeSettingsStore(world);
        check(eye.equals(afterDeath.state(alice).activeEye)&&!afterDeath.state(alice).tracking
                &&afterDeath.state(alice).mode==ConcealmentMode.NONE,"disabled powers remain disabled after restart");
        restarted.state(bob).activeEye=UUID.randomUUID(); restarted.state(bob).mode=ConcealmentMode.NORMAL; restarted.changed();
        EyeSettingsStore multi=new EyeSettingsStore(world);
        check(multi.state(bob).mode==ConcealmentMode.NORMAL&&multi.state(alice).mode==ConcealmentMode.NONE,"saving one player retains another");

        // Malformed JSON is preserved during idle load and archived before the first repair mutation.
        verifyCorruption(root.resolve("broken"),alice,"{broken\r\n",false);
        verifyCorruption(root.resolve("unsupported"),alice,"{\"version\":99,\"players\":{}}",false);
        String mixed="{\"version\":1,\"players\":{\""+alice+"\":{\"activeEye\":\""+eye
                +"\",\"tracking\":true,\"mode\":\"NORMAL\"},\""+bob+"\":{\"tracking\":true,\"mode\":\"INVALID\"}}}";
        verifyCorruption(root.resolve("mixed"),alice,mixed,true);

        // A failed write keeps pending settings for a later retry rather than losing the choice.
        Path blocked=root.resolve("blocked"); EyeSettingsStore retry=new EyeSettingsStore(blocked);
        Files.writeString(blocked,"not a directory"); retry.state(alice).activeEye=eye; retry.state(alice).mode=ConcealmentMode.FULL;
        retry.changed(); check(Files.readString(blocked).equals("not a directory"),"failed save preserves unrelated blocking file");
        Files.delete(blocked); retry.save();
        check(new EyeSettingsStore(blocked).state(alice).mode==ConcealmentMode.FULL,"pending write survives failure and retries");
    }
    private static void verifyCorruption(Path world,UUID owner,String source,boolean validPlayer) throws Exception {
        Path file=world.resolve("data/hunterseye-settings.json"); Files.createDirectories(file.getParent()); Files.writeString(file,source);
        EyeSettingsStore loaded=new EyeSettingsStore(world);
        check(loaded.state(owner).tracking==validPlayer,"invalid records do not enable powers; valid records survive");
        loaded.save(); check(Files.readString(file).equals(source),"idle load/save leaves damaged source bytes intact");
        loaded.state(owner).activeEye=UUID.randomUUID(); loaded.state(owner).tracking=true; loaded.state(owner).mode=ConcealmentMode.FULL;
        loaded.changed();
        try(var paths=Files.list(file.getParent())) {
            var backups=paths.filter(p->p.getFileName().toString().startsWith("hunterseye-settings.json.corrupt-")).toList();
            check(backups.size()==1,"first repair mutation creates one corruption archive");
            check(Files.readString(backups.getFirst()).equals(source),"corruption archive retains exact original bytes");
        }
        check(new EyeSettingsStore(world).state(owner).mode==ConcealmentMode.FULL,"repair produces readable settings");
        loaded.state(owner).mode=ConcealmentMode.NORMAL; loaded.changed();
        try(var paths=Files.list(file.getParent())) {
            check(paths.filter(p->p.getFileName().toString().startsWith("hunterseye-settings.json.corrupt-")).count()==1,
                    "normal later saves do not create redundant corruption archives");
        }
    }
    private static void packets() {
        check(HunterEyeNetworking.CommandPayload.TYPE.id().toString().equals("hunterseye:command"),"command namespace is correct");
        check(HunterEyeNetworking.StatePayload.TYPE.id().toString().equals("hunterseye:state"),"state namespace is correct");
        check(HunterEyeNetworking.ConcealmentPayload.TYPE.id().toString().equals("hunterseye:concealment"),"observer namespace is correct");
        for(var action:HunterEyeNetworking.Action.values()) roundTrip(HunterEyeNetworking.CommandPayload.CODEC,
                new HunterEyeNetworking.CommandPayload(action));
        UUID target=UUID.randomUUID();
        for(var mode:ConcealmentMode.values()) {
            roundTrip(HunterEyeNetworking.ConcealmentPayload.CODEC,new HunterEyeNetworking.ConcealmentPayload(target,mode));
            roundTrip(HunterEyeNetworking.StatePayload.CODEC,new HunterEyeNetworking.StatePayload(true,true,mode,"Tracking",true,
                    target,"Target_Écho","minecraft:the_nether",-123.125,64.5,30000.25,31.999));
        }
        roundTrip(HunterEyeNetworking.StatePayload.CODEC,new HunterEyeNetworking.StatePayload(false,false,ConcealmentMode.NONE,
                "No active eye",false,null,"","",0,0,0,-1));
        roundTrip(HunterEyeNetworking.StatePayload.CODEC,new HunterEyeNetworking.StatePayload(true,true,ConcealmentMode.FULL,
                "Target offline",false,target,"Target_Écho","",0,0,0,-1));
        roundTrip(HunterEyeNetworking.StatePayload.CODEC,new HunterEyeNetworking.StatePayload(true,true,ConcealmentMode.NORMAL,
                "Target in another dimension",true,target,"Other","minecraft:the_end",0,80,0,-1));
        RegistryFriendlyByteBuf invalid=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            invalid.writeVarInt(99);
            boolean rejected=false; try { HunterEyeNetworking.CommandPayload.CODEC.decode(invalid); } catch(RuntimeException expected) { rejected=true; }
            check(rejected,"invalid action ordinal rejected by decoder");
        } finally { invalid.release(); }
    }
    private static <T extends CustomPacketPayload> void roundTrip(StreamCodec<RegistryFriendlyByteBuf,T> codec,T payload) {
        RegistryFriendlyByteBuf buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            codec.encode(buffer,payload); T decoded=codec.decode(buffer);
            check(payload.equals(decoded),"packet round trip retains all fields: "+payload.type().id());
            check(buffer.readableBytes()==0,"decoder consumes exact packet: "+payload.type().id());
        } finally { buffer.release(); }
    }
    private static void check(boolean condition,String message) { checks++; if(!condition) throw new AssertionError(message); }
}
