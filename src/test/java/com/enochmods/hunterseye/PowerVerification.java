package com.enochmods.hunterseye;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;

/** Executable regressions for independent power state, safe persistence and server policy. */
public final class PowerVerification {
    private static int checks;
    public static void main(String[] args) throws Exception {
        transitions();
        cooldowns();
        durations();
        combatPolicy();
        Path root=Files.createTempDirectory("hunterseye-settings-test-");
        try { persistence(root); configuration(root); timerPersistence(root); storageReplacement(root); } finally {
            try(var paths=Files.walk(root)) {
                for(Path path:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }

        System.out.println("Hunter's Eye JVM verification: "+checks+" checks passed.");
    }
    private static void transitions() {
        UUID eye=UUID.randomUUID(); EyePowerState state=new EyePowerState(); state.activeEye=eye;
        EyeConfig config=new EyeConfig(); config.setNormalCooldown(0); config.setFullCooldown(0);
        check(!state.tracking&&state.mode==ConcealmentMode.NONE,"new state has all powers off");
        state.toggle(HunterEyeNetworking.Action.TOGGLE_TRACKING,1000,config);
        state.toggle(HunterEyeNetworking.Action.TOGGLE_NORMAL,1000,config);
        check(state.tracking&&state.mode==ConcealmentMode.NORMAL,"gaze and normal are independent");
        state.toggle(HunterEyeNetworking.Action.TOGGLE_FULL,1000,config);
        check(state.tracking&&state.mode==ConcealmentMode.FULL,"full replaces normal without stopping gaze");
        state.toggle(HunterEyeNetworking.Action.TOGGLE_NORMAL,1000,config);
        check(state.tracking&&state.mode==ConcealmentMode.NORMAL,"normal replaces full without stopping gaze");
        state.toggle(HunterEyeNetworking.Action.TOGGLE_NORMAL,1000,config);
        check(state.tracking&&state.mode==ConcealmentMode.NONE,"normal toggles off independently");
        state.toggle(HunterEyeNetworking.Action.TOGGLE_FULL,1000,config); state.toggle(HunterEyeNetworking.Action.TOGGLE_FULL,1000,config);
        check(state.tracking&&state.mode==ConcealmentMode.NONE,"full toggles off independently");
        state.toggle(HunterEyeNetworking.Action.TOGGLE_FULL,1000,config); state.toggle(HunterEyeNetworking.Action.TOGGLE_TRACKING,1000,config);
        check(!state.tracking&&state.mode==ConcealmentMode.FULL,"turning gaze off keeps full");
        state.tracking=true; state.disable();
        check(!state.tracking&&state.mode==ConcealmentMode.NONE,"death/loss disable both powers");
        check(eye.equals(state.activeEye),"disable retains selected eye identity for explicit reactivation");
    }
    private static void cooldowns() {
        EyePowerState state=new EyePowerState(); EyeConfig config=new EyeConfig();
        check(state.requestMode(ConcealmentMode.NORMAL,1_000,config.normalCooldown()),"first Normal activation succeeds");
        check(state.normalCooldownUntil==31_000,"Normal starts its 30-second deadline at activation");
        check(state.remainingSeconds(ConcealmentMode.NORMAL,1_001)==30,"remaining time rounds fractional seconds up");
        check(state.requestMode(ConcealmentMode.NORMAL,2_000,3600)&&state.normalCooldownUntil==31_000,"repeated explicit mode neither blocks nor charges");
        check(state.requestMode(ConcealmentMode.FULL,3_000,config.fullCooldown()),"Full uses an independent cooldown");
        check(state.modeUntil==33_000,"switching modes replaces the old effect deadline with the new duration");
        check(state.fullCooldownUntil==33_000&&state.normalCooldownUntil==31_000,"Full starts its 30-second deadline without modifying Normal");
        check(!state.requestMode(ConcealmentMode.NORMAL,4_000,120)&&state.mode==ConcealmentMode.FULL,"blocked transition preserves the current mode");
        check(state.modeUntil==33_000,"blocked activation cannot alter the active effect deadline");
        check(state.normalCooldownUntil==31_000&&state.fullCooldownUntil==33_000,"blocked transition preserves both deadlines");
        check(state.toggle(HunterEyeNetworking.Action.TOGGLE_FULL,5_000,config)&&state.mode==ConcealmentMode.NONE,"gesture off is always allowed while cooling");
        check(!state.toggle(HunterEyeNetworking.Action.TOGGLE_FULL,6_000,config),"gesture activation shares the cooldown check");
        check(state.requestMode(ConcealmentMode.NONE,7_000,30),"explicit off is always allowed");
        check(state.remainingSeconds(ConcealmentMode.FULL,32_999)==1,"one real millisecond remaining reports one second");
        check(state.remainingSeconds(ConcealmentMode.FULL,33_000)==0,"deadline boundary is ready");
        check(state.requestMode(ConcealmentMode.FULL,33_000,30),"activation at the exact deadline succeeds");
        state.disable(); state.activeEye=UUID.randomUUID();
        check(!state.requestMode(ConcealmentMode.FULL,34_000,30),"death or changing the selected Eye cannot bypass cooldown");
        check(state.normalCooldownUntil==31_000&&state.fullCooldownUntil==63_000,"disable retains player-owned deadlines");
        EyePowerState other=new EyePowerState();
        check(other.requestMode(ConcealmentMode.NORMAL,34_000,120),"another player has independent activation eligibility");
        check(state.requestMode(ConcealmentMode.NORMAL,121_000,0),"zero configured cooldown permits activation");
        state.requestMode(ConcealmentMode.NONE,121_000,0);
        check(state.requestMode(ConcealmentMode.NORMAL,121_000,0),"zero cooldown permits immediate reactivation");
    }
    private static void durations() {
        EyePowerState state=new EyePowerState(); EyeConfig config=new EyeConfig(); state.tracking=true;
        check(state.requestMode(ConcealmentMode.NORMAL,1_000,30,120),"Normal timed activation succeeds");
        check(state.modeUntil==121_000&&state.remainingDurationSeconds(1_001)==120,"Normal effect lasts two minutes with rounded countdown");
        long normalCooldown=state.normalCooldownUntil;
        check(state.requestMode(ConcealmentMode.NORMAL,31_001,0,3600)&&state.modeUntil==121_000
                &&state.normalCooldownUntil==normalCooldown,"same-mode command cannot extend effect even after cooldown elapsed or settings changed");
        check(!state.expire(120_999)&&state.remainingDurationSeconds(120_999)==1,"effect persists until its exact deadline");
        check(state.expire(121_000)&&state.mode==ConcealmentMode.NONE&&state.modeUntil==0,"effect expires exactly at deadline");
        check(state.tracking&&state.normalCooldownUntil==normalCooldown,"expiry preserves Gaze and player cooldown");
        check(!state.expire(121_001),"expiry is idempotent");
        check(state.requestMode(ConcealmentMode.FULL,122_000,60,30)&&state.modeUntil==152_000,"Full effect lasts thirty seconds independently of cooldown");
        long fullCooldown=state.fullCooldownUntil;
        check(!state.requestMode(ConcealmentMode.FULL,152_000,60,30)&&state.mode==ConcealmentMode.NONE,"expired same-mode command observes outstanding cooldown");
        check(state.fullCooldownUntil==fullCooldown&&state.tracking,"rejected expired request preserves cooldown and Gaze");
        check(state.requestMode(ConcealmentMode.NORMAL,153_000,0,1)&&state.modeUntil==154_000,"configured one-second duration is honored");
        check(state.toggle(HunterEyeNetworking.Action.TOGGLE_NORMAL,154_000,config)&&state.mode==ConcealmentMode.NORMAL
                &&state.modeUntil==274_000,"expired gesture activates a new timer instead of toggling stale mode off");
        check(state.requestMode(ConcealmentMode.NONE,155_000,0,1)&&state.modeUntil==0&&state.tracking,"off clears only effect deadline and mode");
        state.requestMode(ConcealmentMode.FULL,182_000,30,30); state.disable();
        check(state.modeUntil==0&&!state.tracking&&state.fullCooldownUntil==212_000,"death/loss clears effect while retaining cooldown");
        EyePowerState saturated=new EyePowerState();
        check(saturated.requestMode(ConcealmentMode.NORMAL,Long.MAX_VALUE-1,3600,3600)
                &&saturated.modeUntil==Long.MAX_VALUE&&saturated.normalCooldownUntil==Long.MAX_VALUE,"timer additions saturate safely");
        check(saturated.remainingDurationSeconds(Long.MAX_VALUE-1)==1&&saturated.expire(Long.MAX_VALUE),"saturated deadline still expires at exact boundary");
        EyePowerState inactive=new EyePowerState(); inactive.modeUntil=Long.MAX_VALUE; inactive.tracking=true;
        check(inactive.restoreTimer(1_000,config)&&inactive.modeUntil==0&&inactive.tracking,"restore removes stale deadline from inactive mode without changing Gaze");
    }
    private static void timerPersistence(Path root) throws Exception {
        UUID owner=UUID.randomUUID(),eye=UUID.randomUUID(); EyeConfig config=new EyeConfig();
        Path world=root.resolve("timer-world"); EyeSettingsStore first=new EyeSettingsStore(world);
        EyePowerState active=first.state(owner); active.activeEye=eye; active.tracking=true;
        active.requestMode(ConcealmentMode.FULL,1_000,60,30); first.changed();
        EyeSettingsStore restarted=new EyeSettingsStore(world); restarted.restoreTimers(10_000,config);
        check(restarted.state(owner).modeUntil==31_000&&restarted.state(owner).remainingDurationSeconds(10_000)==21,
                "restart uses remaining wall-clock duration without restarting effect");
        restarted.restoreTimers(31_000,config);
        EyePowerState expired=new EyeSettingsStore(world).state(owner);
        check(expired.mode==ConcealmentMode.NONE&&expired.modeUntil==0&&expired.tracking&&expired.fullCooldownUntil==61_000,
                "offline expiry is persisted while retaining Gaze and cooldown");
        Path legacy=root.resolve("legacy-timer/data/hunterseye-settings.json"); Files.createDirectories(legacy.getParent());
        Files.writeString(legacy,"{\"version\":1,\"players\":{\""+owner+"\":{\"activeEye\":\""+eye+"\",\"tracking\":true,\"mode\":\"NORMAL\",\"normalCooldownUntil\":999999}}}");
        EyeSettingsStore migrated=new EyeSettingsStore(root.resolve("legacy-timer")); config.setNormalDuration(90);
        migrated.restoreTimers(100_000,config);
        check(migrated.state(owner).modeUntil==190_000&&migrated.state(owner).normalCooldownUntil==999999,
                "legacy active mode receives one bounded configured timer without resetting cooldown");
        config.setNormalDuration(3600);
        EyeSettingsStore again=new EyeSettingsStore(root.resolve("legacy-timer")); again.restoreTimers(150_000,config);
        check(again.state(owner).modeUntil==190_000,"legacy migration persists deadline so subsequent restarts/config edits cannot extend it");
        again.restoreTimers(200_000,config);
        check(new EyeSettingsStore(root.resolve("legacy-timer")).state(owner).mode==ConcealmentMode.NONE,"legacy migrated effect expires offline");
    }
    private static void persistence(Path root) throws Exception {
        UUID alice=UUID.randomUUID(),bob=UUID.randomUUID(),eye=UUID.randomUUID();
        Path world=root.resolve("world-a"); EyeSettingsStore first=new EyeSettingsStore(world);
        check(!first.state(alice).tracking&&first.state(alice).mode==ConcealmentMode.NONE,"absent settings fail closed");
        first.state(alice).activeEye=eye; first.state(alice).tracking=true; first.state(alice).mode=ConcealmentMode.FULL;
        first.state(alice).modeUntil=60_000;
        first.state(alice).normalCooldownUntil=1_234_567; first.state(alice).fullCooldownUntil=2_345_678;
        first.changed();
        Path file=world.resolve("data/hunterseye-settings.json");
        check(Files.exists(file),"mutation immediately writes settings");
        check(!Files.exists(file.resolveSibling("hunterseye-settings.json.tmp")),"successful atomic save consumes temporary file");
        EyeSettingsStore restarted=new EyeSettingsStore(world);
        check(eye.equals(restarted.state(alice).activeEye),"restart preserves selected stack identity");
        check(restarted.state(alice).tracking&&restarted.state(alice).mode==ConcealmentMode.FULL,"restart preserves both power choices");
        check(restarted.state(alice).modeUntil==60_000,"restart preserves the active effect deadline");
        check(restarted.state(alice).normalCooldownUntil==1_234_567&&restarted.state(alice).fullCooldownUntil==2_345_678,"restart preserves per-mode real-time deadlines");
        check(restarted.state(alice).requestMode(ConcealmentMode.FULL,1_000,30)&&restarted.state(alice).fullCooldownUntil==2_345_678,"restored active mode does not start a new cooldown");
        check(restarted.state(bob).activeEye==null&&!restarted.state(bob).tracking,"players remain isolated");
        check(new EyeSettingsStore(root.resolve("world-b")).state(alice).activeEye==null,"worlds remain isolated");
        restarted.state(alice).disable(); restarted.changed();
        EyeSettingsStore afterDeath=new EyeSettingsStore(world);
        check(eye.equals(afterDeath.state(alice).activeEye)&&!afterDeath.state(alice).tracking
                &&afterDeath.state(alice).mode==ConcealmentMode.NONE,"disabled powers remain disabled after restart");
        check(afterDeath.state(alice).modeUntil==0,"death clears the persisted effect deadline");
        check(afterDeath.state(alice).normalCooldownUntil==1_234_567&&afterDeath.state(alice).fullCooldownUntil==2_345_678,"death and restart retain cooldown deadlines");
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
    private static void combatPolicy() {
        for(ConcealmentMode mode:ConcealmentMode.values()) {
            check(EyePolicy.isFullNonCombat(mode,true)==(mode==ConcealmentMode.FULL),"only eligible Full blocks outgoing combat: "+mode);
            check(!EyePolicy.isFullNonCombat(mode,false),"missing Eye cannot retain noncombat: "+mode);
        }
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
    private static void configuration(Path root) throws Exception {
        EyeConfig config=new EyeConfig(root.resolve("configured"));
        check(config.bindRange()==32&&config.trackingRange()==128&&!config.crossDimension()&&config.color()==0xFF0000,"configuration defaults");
        check(config.normalCooldown()==30&&config.fullCooldown()==30,"cooldown defaults use real seconds");
        check(config.normalDuration()==120&&config.fullDuration()==30,"effect defaults are two minutes Normal and thirty seconds Full");
        check(!EyeConfig.validDuration(0)&&!EyeConfig.validDuration(3601)&&EyeConfig.validDuration(1)&&EyeConfig.validDuration(3600),"duration bounds are one second through one hour");
        for(int value:new int[]{0,3601}) {
            boolean normalRejected=false,fullRejected=false;
            try { config.setNormalDuration(value); } catch(IllegalArgumentException expected) { normalRejected=true; }
            try { config.setFullDuration(value); } catch(IllegalArgumentException expected) { fullRejected=true; }
            check(normalRejected&&fullRejected&&config.normalDuration()==120&&config.fullDuration()==30,"invalid duration changes are rejected: "+value);
        }
        check(!EyeConfig.validBindRange(0)&&!EyeConfig.validBindRange(129)&&EyeConfig.validBindRange(128),"bind bounds");
        check(!EyeConfig.validTrackingRange(0)&&!EyeConfig.validTrackingRange(30_000_001)&&EyeConfig.validTrackingRange(30_000_000),"tracking bounds");
        check(EyePolicy.canTrack(true,128,config),"tracking includes the range boundary");
        check(!EyePolicy.canTrack(true,128.01,config),"tracking excludes beyond the boundary");
        check(!EyePolicy.canTrack(false,10,config),"tracking blocks other dimensions by default");
        check(!EyeConfig.validCooldown(-1)&&!EyeConfig.validCooldown(3601)&&EyeConfig.validCooldown(0)&&EyeConfig.validCooldown(3600),"cooldown bounds include zero and one hour");
        for(int invalid:new int[]{-1,3601}) {
            boolean normalRejected=false,fullRejected=false;
            try { config.setNormalCooldown(invalid); } catch(IllegalArgumentException expected) { normalRejected=true; }
            try { config.setFullCooldown(invalid); } catch(IllegalArgumentException expected) { fullRejected=true; }
            check(normalRejected&&fullRejected&&config.normalCooldown()==30&&config.fullCooldown()==30,"invalid cooldown leaves defaults unchanged: "+invalid);
        }
        config.setBindRange(64); config.setTrackingRange(500); config.setCrossDimension(true); config.setColor("ab12Cd");
        config.setNormalCooldown(0); config.setFullCooldown(3600);
        config.setNormalDuration(3600); config.setFullDuration(1);
        EyeConfig restored=new EyeConfig(root.resolve("configured"));
        check(restored.bindRange()==64&&restored.trackingRange()==500&&restored.crossDimension()&&restored.color()==0xAB12CD,"configuration persists independently");
        check(restored.normalDuration()==3600&&restored.fullDuration()==1,"both effect duration boundaries persist");
        check(restored.describe().contains("normalDuration=3600s")&&restored.describe().contains("fullDuration=1s"),"config show reports duration seconds");
        check(restored.normalCooldown()==0&&restored.fullCooldown()==3600,"both cooldown boundaries persist");
        check(restored.describe().contains("normalCooldown=0s")&&restored.describe().contains("fullCooldown=3600s"),"config show reports cooldown seconds");
        check(EyePolicy.canTrack(false,Double.POSITIVE_INFINITY,restored),"cross-dimension enabled has no meaningful scalar distance limit");
        String[] invalid={"#112233","12345","GGGGGG","1234567"};
        for(String value:invalid) {
            boolean rejected=false; try { restored.setColor(value); } catch(IllegalArgumentException expected) { rejected=true; }
            check(rejected,"invalid color rejected: "+value);
        }
        restored.reset();
        check(new EyeConfig(root.resolve("configured")).describe().equals(new EyeConfig().describe()),"reset persists defaults");
        Path bad=root.resolve("badconfig/data/hunterseye-config.json"); Files.createDirectories(bad.getParent()); Files.writeString(bad,"{broken");
        EyeConfig damaged=new EyeConfig(root.resolve("badconfig"));
        check(damaged.describe().equals(new EyeConfig().describe()),"corrupt config falls back to defaults");
        check(Files.readString(bad).equals("{broken"),"corrupt config remains intact during read");
        damaged.setBindRange(40);
        try(var paths=Files.list(bad.getParent())) {
            var backups=paths.filter(p->p.getFileName().toString().startsWith("hunterseye-config.json.corrupt-")).toList();
            check(backups.size()==1&&Files.readString(backups.getFirst()).equals("{broken"),"config repair preserves exact corrupt bytes");
        }
        Path blocked=root.resolve("config-save-blocked"); EyeConfig retry=new EyeConfig(blocked);
        Files.writeString(blocked,"blocking file");
        boolean rejected=false;
        try { retry.setBindRange(64); } catch(IllegalStateException expected) { rejected=true; }
        check(rejected&&retry.bindRange()==32,"failed config save reports failure and rolls back live value");
        boolean normalRejected=false,fullRejected=false;
        try { retry.setNormalCooldown(10); } catch(IllegalStateException expected) { normalRejected=true; }
        try { retry.setFullCooldown(20); } catch(IllegalStateException expected) { fullRejected=true; }
        check(normalRejected&&fullRejected&&retry.normalCooldown()==30&&retry.fullCooldown()==30,"failed cooldown writes roll back both live values");
        boolean normalDurationRejected=false,fullDurationRejected=false;
        try { retry.setNormalDuration(1); } catch(IllegalStateException expected) { normalDurationRejected=true; }
        try { retry.setFullDuration(3600); } catch(IllegalStateException expected) { fullDurationRejected=true; }
        check(normalDurationRejected&&fullDurationRejected&&retry.normalDuration()==120&&retry.fullDuration()==30,"failed duration writes roll back both runtime settings");
        String beforeReset=retry.describe(); boolean resetRejected=false;
        try { retry.reset(); } catch(IllegalStateException expected) { resetRejected=true; }
        check(resetRejected&&retry.describe().equals(beforeReset),"failed reset rolls back every runtime setting including effect durations");
        check(Files.readString(blocked).equals("blocking file"),"failed config save preserves unrelated file");
        Files.delete(blocked); retry.setBindRange(64);
        check(new EyeConfig(blocked).bindRange()==64,"config can be changed and persisted after storage recovery");
    }
    private static void storageReplacement(Path root) throws Exception {
        Path temporary=root.resolve("replace.tmp"),destination=root.resolve("replace.json");
        Files.writeString(temporary,"new settings"); Files.writeString(destination,"original settings");
        int[] attempts={0}; List<Long> delays=new ArrayList<>();
        EyeStorage.replace(temporary,destination,(source,target,options)->{
            check(Files.readString(target).equals("original settings"),"denied replacement attempts preserve destination bytes");
            if(++attempts[0]<=3) throw new AccessDeniedException(target.toString());
            Files.move(source,target,options);
        },delays::add);
        check(attempts[0]==4&&delays.equals(List.of(10L,25L,50L)),"transient access denial retries at most three times with 85ms total delay");
        check(Files.readString(destination).equals("new settings")&&!Files.exists(temporary),"successful retry replaces destination and consumes temporary file");

        Files.writeString(temporary,"pending settings"); attempts[0]=0; delays.clear(); boolean denied=false;
        try {
            EyeStorage.replace(temporary,destination,(source,target,options)->{
                attempts[0]++; throw new AccessDeniedException(target.toString());
            },delays::add);
        } catch(AccessDeniedException expected) { denied=true; }
        check(denied&&attempts[0]==4&&delays.equals(List.of(10L,25L,50L)),"permanent access denial returns final failure after bounded retries");
        check(Files.readString(destination).equals("new settings")&&Files.readString(temporary).equals("pending settings"),"exhausted retries preserve original and pending settings bytes");

        attempts[0]=0; delays.clear(); boolean missing=false;
        try {
            EyeStorage.replace(temporary,destination,(source,target,options)->{
                attempts[0]++; throw new NoSuchFileException(source.toString());
            },delays::add);
        } catch(NoSuchFileException expected) { missing=true; }
        check(missing&&attempts[0]==1&&delays.isEmpty(),"other I/O failures propagate immediately without retry");

        attempts[0]=0; delays.clear();
        EyeStorage.replace(temporary,destination,(source,target,options)->{
            attempts[0]++;
            if(options.length==2) throw new AtomicMoveNotSupportedException(source.toString(),target.toString(),"fixture filesystem");
            Files.move(source,target,options);
        },delays::add);
        check(attempts[0]==2&&delays.isEmpty()&&Files.readString(destination).equals("pending settings"),"unsupported atomic move retains existing replace fallback");

        Files.writeString(temporary,"interrupted pending settings"); attempts[0]=0; boolean interrupted=false;
        try {
            EyeStorage.replace(temporary,destination,(source,target,options)->{
                attempts[0]++; throw new AccessDeniedException(target.toString());
            },millis->{ throw new InterruptedException("fixture interrupt"); });
        } catch(java.io.IOException expected) {
            interrupted=expected.getCause() instanceof InterruptedException&&Thread.currentThread().isInterrupted();
        } finally { Thread.interrupted(); }
        check(interrupted&&attempts[0]==1,"interrupted retry restores thread flag and returns I/O failure without further attempts");
        check(Files.readString(destination).equals("pending settings")&&Files.exists(temporary),"interrupted retry preserves destination and temporary file");
    }
    private static void check(boolean condition,String message) { checks++; if(!condition) throw new AssertionError(message); }
}
