package com.enochmods.hunterseye;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The path comes from the active world's storage, never a global game directory. */
final class EyeSettingsStore {
    final Map<UUID,EyePowerState> states=new HashMap<>();
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER=LoggerFactory.getLogger("hunterseye");
    private final Path path;
    private boolean dirty;
    private boolean preserveOriginal;
    EyeSettingsStore(Path worldRoot) { path=worldRoot.resolve("data/hunterseye-settings.json"); load(); }
    EyePowerState state(UUID owner) { return states.computeIfAbsent(owner,id->new EyePowerState()); }
    void changed() { dirty=true; save(); }
    void restoreTimers(long nowMillis,EyeConfig config) {
        boolean changed=false;
        for(EyePowerState state:states.values()) changed|=state.restoreTimer(nowMillis,config);
        if(changed) changed();
    }
    private void load() {
        if(!Files.exists(path)) return;
        try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)) {
            JsonObject root=JsonParser.parseReader(reader).getAsJsonObject();
            if(root.get("version").getAsInt()!=1) throw new IOException("Unsupported settings version");
            JsonObject players=root.getAsJsonObject("players");
            for(var entry:players.entrySet()) {
                try {
                    UUID owner=UUID.fromString(entry.getKey()); JsonObject data=entry.getValue().getAsJsonObject();
                    EyePowerState state=new EyePowerState();
                    if(data.has("activeEye")) state.activeEye=UUID.fromString(data.get("activeEye").getAsString());
                    state.tracking=data.get("tracking").getAsBoolean();
                    state.mode=ConcealmentMode.valueOf(data.get("mode").getAsString());
                    state.normalCooldownUntil=data.has("normalCooldownUntil")?data.get("normalCooldownUntil").getAsLong():0;
                    state.fullCooldownUntil=data.has("fullCooldownUntil")?data.get("fullCooldownUntil").getAsLong():0;
                    state.modeUntil=data.has("modeUntil")?data.get("modeUntil").getAsLong():0;
                    if(state.modeUntil<0||state.normalCooldownUntil<0||state.fullCooldownUntil<0) throw new IllegalArgumentException("Invalid power deadline");
                    states.put(owner,state);
                } catch(RuntimeException error) {
                    preserveOriginal=true;
                    LOGGER.warn("Ignoring invalid Hunter's Eye settings for {}",entry.getKey());
                }
            }
        } catch(IOException|RuntimeException error) {
            preserveOriginal=true;
            LOGGER.error("Unable to load Hunter's Eye settings; powers remain disabled",error);
        }
    }
    void save() {
        if(!dirty) return;
        JsonObject root=new JsonObject(),players=new JsonObject(); root.addProperty("version",1);
        states.forEach((owner,state)->{
            JsonObject data=new JsonObject(); if(state.activeEye!=null) data.addProperty("activeEye",state.activeEye.toString());
            data.addProperty("tracking",state.tracking); data.addProperty("mode",state.mode.name()); players.add(owner.toString(),data);
            data.addProperty("modeUntil",state.modeUntil);
            data.addProperty("normalCooldownUntil",state.normalCooldownUntil); data.addProperty("fullCooldownUntil",state.fullCooldownUntil);
        });
        root.add("players",players);
        Path temporary=path.resolveSibling(path.getFileName()+".tmp");
        try {
            Files.createDirectories(path.getParent());
            if(preserveOriginal && Files.exists(path)) {
                // Preserve exact source bytes before the first repair write. If this fails,
                // retain dirty state and the original file so a later save can retry safely.
                Path backup=path.resolveSibling(path.getFileName()+".corrupt-"+UUID.randomUUID()+".bak");
                Files.copy(path,backup);
                preserveOriginal=false;
                LOGGER.warn("Preserved invalid Hunter's Eye settings in {}",backup);
            }
            Files.writeString(temporary,GSON.toJson(root),StandardCharsets.UTF_8);
            EyeStorage.replace(temporary,path);
            dirty=false;
        } catch(IOException error) { LOGGER.error("Unable to save Hunter's Eye settings",error); }
    }
}
