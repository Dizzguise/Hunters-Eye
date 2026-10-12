package com.enochmods.hunterseye;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Operator configuration is stored separately from per-player power preferences. */
public final class EyeConfig {
    private static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger("hunterseye");
    public static final int DEFAULT_BIND_RANGE=32, DEFAULT_TRACKING_RANGE=128, DEFAULT_HIGHLIGHT_RANGE=64, DEFAULT_COLOR=0xFF0000;
    public static final int DEFAULT_NORMAL_COOLDOWN=30, DEFAULT_FULL_COOLDOWN=30;
    public static final int DEFAULT_NORMAL_DURATION=120, DEFAULT_FULL_DURATION=30;
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private int bindRange=DEFAULT_BIND_RANGE, trackingRange=DEFAULT_TRACKING_RANGE, highlightRange=DEFAULT_HIGHLIGHT_RANGE, color=DEFAULT_COLOR;
    private boolean crossDimension;
    private int normalCooldown=DEFAULT_NORMAL_COOLDOWN, fullCooldown=DEFAULT_FULL_COOLDOWN;
    private int normalDuration=DEFAULT_NORMAL_DURATION, fullDuration=DEFAULT_FULL_DURATION;
    private transient Path path;
    private transient boolean preserveOriginal;
    public EyeConfig() {}
    public EyeConfig(Path worldRoot) {
        path=worldRoot.resolve("data/hunterseye-config.json");
        if(!Files.exists(path)) return;
        try {
            EyeConfig loaded=GSON.fromJson(Files.readString(path,StandardCharsets.UTF_8),EyeConfig.class);
            if(loaded==null||!validBindRange(loaded.bindRange)||!validTrackingRange(loaded.trackingRange)
                    ||!validHighlightRange(loaded.highlightRange)
                    ||!validCooldown(loaded.normalCooldown)||!validCooldown(loaded.fullCooldown)
                    ||!validDuration(loaded.normalDuration)||!validDuration(loaded.fullDuration)
                    ||loaded.color<0||loaded.color>0xFFFFFF) throw new IOException("Invalid configuration");
            bindRange=loaded.bindRange; trackingRange=loaded.trackingRange; highlightRange=loaded.highlightRange; crossDimension=loaded.crossDimension; color=loaded.color;
            normalCooldown=loaded.normalCooldown; fullCooldown=loaded.fullCooldown;
            normalDuration=loaded.normalDuration; fullDuration=loaded.fullDuration;
        } catch(IOException|RuntimeException error) {
            preserveOriginal=true;
            LOGGER.error("Unable to load Hunter's Eye configuration; using defaults",error);
        }
    }
    public int bindRange() { return bindRange; }
    public int trackingRange() { return trackingRange; }
    public int highlightRange() { return highlightRange; }
    public boolean crossDimension() { return crossDimension; }
    public int color() { return color; }
    public int normalCooldown() { return normalCooldown; }
    public int fullCooldown() { return fullCooldown; }
    public int normalDuration() { return normalDuration; }
    public int fullDuration() { return fullDuration; }
    public static boolean validDuration(int value) { return value>=1&&value<=3600; }
    public static boolean validCooldown(int value) { return value>=0&&value<=3600; }
    public static boolean validBindRange(int value) { return value>=1&&value<=128; }
    public static boolean validTrackingRange(int value) { return value>=1&&value<=30_000_000; }
    public static boolean validHighlightRange(int value) { return value>=1&&value<=512; }
    public void setBindRange(int value) { if(!validBindRange(value)) throw new IllegalArgumentException("Bind range must be 1..128"); update(()->bindRange=value); }
    public void setTrackingRange(int value) { if(!validTrackingRange(value)) throw new IllegalArgumentException("Tracking range must be 1..30000000"); update(()->trackingRange=value); }
    public void setHighlightRange(int value) { if(!validHighlightRange(value)) throw new IllegalArgumentException("Highlight range must be 1..512"); update(()->highlightRange=value); }
    public void setCrossDimension(boolean value) { update(()->crossDimension=value); }
    public void setNormalCooldown(int value) { if(!validCooldown(value)) throw new IllegalArgumentException("Normal cooldown must be 0..3600 seconds"); update(()->normalCooldown=value); }
    public void setFullCooldown(int value) { if(!validCooldown(value)) throw new IllegalArgumentException("Full cooldown must be 0..3600 seconds"); update(()->fullCooldown=value); }
    public void setNormalDuration(int value) { if(!validDuration(value)) throw new IllegalArgumentException("Normal duration must be 1..3600 seconds"); update(()->normalDuration=value); }
    public void setFullDuration(int value) { if(!validDuration(value)) throw new IllegalArgumentException("Full duration must be 1..3600 seconds"); update(()->fullDuration=value); }
    public void setColor(String value) {
        if(!value.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Color must be exactly six hexadecimal digits");
        update(()->color=Integer.parseInt(value,16));
    }
    public void reset() {
        update(()->{ bindRange=DEFAULT_BIND_RANGE; trackingRange=DEFAULT_TRACKING_RANGE; highlightRange=DEFAULT_HIGHLIGHT_RANGE; crossDimension=false; color=DEFAULT_COLOR; normalCooldown=DEFAULT_NORMAL_COOLDOWN; fullCooldown=DEFAULT_FULL_COOLDOWN; normalDuration=DEFAULT_NORMAL_DURATION; fullDuration=DEFAULT_FULL_DURATION; });
    }
    private void update(Runnable mutation) {
        int oldBind=bindRange,oldTracking=trackingRange,oldHighlight=highlightRange,oldColor=color; boolean oldCross=crossDimension;
        int oldNormalCooldown=normalCooldown,oldFullCooldown=fullCooldown;
        int oldNormalDuration=normalDuration,oldFullDuration=fullDuration;
        mutation.run();
        if(!save()) {
            bindRange=oldBind; trackingRange=oldTracking; highlightRange=oldHighlight; color=oldColor; crossDimension=oldCross;
            normalCooldown=oldNormalCooldown; fullCooldown=oldFullCooldown;
            normalDuration=oldNormalDuration; fullDuration=oldFullDuration;
            throw new IllegalStateException("Unable to save Hunter's Eye configuration; setting unchanged. Check server logs and world storage.");
        }
    }
    public String describe() {
        return "bindRange="+bindRange+", trackingRange="+trackingRange+", highlightRange="+highlightRange+", crossDimension="+crossDimension+", color="+String.format("%06X",color)+", normalCooldown="+normalCooldown+"s, fullCooldown="+fullCooldown+"s, normalDuration="+normalDuration+"s, fullDuration="+fullDuration+"s";
    }
    public boolean save() {
        if(path==null) return true;
        Path temporary=path.resolveSibling(path.getFileName()+".tmp");
        try {
            Files.createDirectories(path.getParent());
            if(preserveOriginal&&Files.exists(path)) {
                Files.copy(path,path.resolveSibling(path.getFileName()+".corrupt-"+UUID.randomUUID()+".bak"));
                preserveOriginal=false;
            }
            Files.writeString(temporary,GSON.toJson(this),StandardCharsets.UTF_8);
            EyeStorage.replace(temporary,path);
            return true;
        } catch(IOException error) { LOGGER.error("Unable to save Hunter's Eye configuration",error); return false; }
    }
}
