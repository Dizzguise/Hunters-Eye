package com.enochmods.hunterseye;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Packet gesture regressions and migration/transaction checks for the new marker setting. */
public final class ControlVerification {
    private static int checks;
    public static void main(String[] args) throws Exception {
        gestures();
        Path root=Files.createTempDirectory("hunterseye-controls-test-");
        try { configuration(root); } finally {
            try(var paths=Files.walk(root)) {
                for(Path path:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
        System.out.println("Hunter's Eye controls verification: "+checks+" checks passed.");
    }
    private static void gestures() {
        EyeGestureGuard guard=new EyeGestureGuard();
        check(guard.accept(0),"first air click toggles");
        check(!guard.accept(0),"attack and its swing toggle once");
        check(!guard.accept(1),"neighboring packet does not toggle twice");
        check(guard.accept(6),"a quiet interval rearms the next click");
        guard=new EyeGestureGuard();
        check(guard.accept(100),"block start toggles");
        for(int tick=100;tick<200;tick++) check(!guard.accept(tick),"held block swing cannot toggle again: "+tick);
        guard.continuation(200);
        check(!guard.accept(201),"stop/abort and its swing remain one gesture");
        check(guard.accept(206),"next block/air/entity gesture rearms");
        guard=new EyeGestureGuard(); guard.suppressUse(10);
        check(!guard.accept(10),"sneak right-click bind swing does not toggle Gaze");
        check(!guard.accept(11),"drop prediction follow-up swing is suppressed");
        check(guard.accept(16),"next deliberate left click works after drop/use");
        guard.suppressUse(20); guard.suppressUse(22);
        check(!guard.accept(23),"held right-click keeps suppressing its swings");
    }
    private static void configuration(Path root) throws Exception {
        EyeConfig config=new EyeConfig(root.resolve("current"));
        check(config.highlightRange()==64,"highlight default is 64");
        check(!EyeConfig.validHighlightRange(0)&&EyeConfig.validHighlightRange(1)
                &&EyeConfig.validHighlightRange(512)&&!EyeConfig.validHighlightRange(513),"highlight bounds include 1 and 512");
        config.setHighlightRange(512);
        check(new EyeConfig(root.resolve("current")).highlightRange()==512,"highlight choice persists");
        check(config.trackingRange()==128,"highlight choice leaves tracking policy independent");
        config.reset();
        check(new EyeConfig(root.resolve("current")).highlightRange()==64,"reset restores persisted highlight default");
        Path legacy=root.resolve("legacy/data/hunterseye-config.json");
        Files.createDirectories(legacy.getParent());
        Files.writeString(legacy,"{\"bindRange\":40,\"trackingRange\":500,\"crossDimension\":true,\"color\":11259375}");
        EyeConfig migrated=new EyeConfig(root.resolve("legacy"));
        check(migrated.highlightRange()==64&&migrated.bindRange()==40&&migrated.trackingRange()==500
                &&migrated.crossDimension()&&migrated.color()==11259375,"0.2.0 config gains default without changing prior choices");
        check(migrated.normalCooldown()==30&&migrated.fullCooldown()==30,"legacy config gains default cooldowns");
        check(migrated.normalDuration()==120&&migrated.fullDuration()==30,"legacy config gains bounded default durations");
        Path prior=root.resolve("prior/data/hunterseye-config.json"); Files.createDirectories(prior.getParent());
        Files.writeString(prior,"{\"normalCooldown\":120,\"fullCooldown\":45}");
        EyeConfig choices=new EyeConfig(root.resolve("prior"));
        check(choices.normalCooldown()==120&&choices.fullCooldown()==45&&choices.normalDuration()==120&&choices.fullDuration()==30,
                "adding duration fields preserves existing explicitly saved cooldown choices");
        Path blocked=root.resolve("blocked"); EyeConfig retry=new EyeConfig(blocked);
        Files.writeString(blocked,"blocking file");
        boolean rejected=false;
        try { retry.setHighlightRange(99); } catch(IllegalStateException expected) { rejected=true; }
        check(rejected&&retry.highlightRange()==64,"failed highlight save rolls back runtime value");
        check(Files.readString(blocked).equals("blocking file"),"failed save preserves unrelated file");
        Files.delete(blocked); retry.setHighlightRange(99);
        check(new EyeConfig(blocked).highlightRange()==99,"recovered storage persists highlight choice");
    }
    private static void check(boolean condition,String message) { checks++; if(!condition) throw new AssertionError(message); }
}
