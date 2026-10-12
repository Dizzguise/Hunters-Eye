# Hunter's Eye

**Minecraft Java 26.2 / Fabric**, version **0.2.5**. Install this mod on the server; players may connect with ordinary vanilla clients. It also works in a Fabric single-player instance. Hunter's Gaze and Phantom Hunt use vanilla items, controls, commands, action bars, entity metadata, and entity tracking packets. No client mod is required. An optional 0.2.4 resource pack gives the Eyes their original artwork; the Gaze box needs no resource pack.

## Install and obtain

Download the current server mod, optional client helper, and artwork pack from [Releases](https://github.com/Dizzguise/Hunters-Eye/releases/latest).

Use Java 25, Fabric Loader **0.19.3** or newer, and Fabric API **0.161.0+26.2**. Copy the built hunters-eye-0.2.5.jar and matching Fabric API jar to the server's mods folder, then restart. For single-player, put those jars in the local Fabric instance. This project has no dependency on Enoch or Ghost Slots.

An operator with gamemaster permissions (normally operator level 2+) can mint an Eye:

    /hunterseye give eye [player]

Omit the optional player to give the item to yourself. A server console must name the player. The player must have a free inventory slot. These are tagged vanilla Ender Eyes with a unique saved identity, a one-item stack limit, a custom name, and instructions in their lore. A regular vanilla Ender Eye has no powers. The Eye has a violet name and uses the vanilla Ender Eye texture without the artwork pack. There is no creative-tab entry or crafting recipe.

The optional [resource pack](resource-pack/README.md) contains the original violet-and-gold Eye artwork. Source assets live under `resource-pack/pack`; the built pack is `build/resourcepacks/hunters-eye-resource-pack-26.2-0.2.4.zip`. A server can offer or require its hosted HTTPS pack with vanilla resource-pack, resource-pack-sha1, and require-resource-pack settings; clients need no helper mod for textures or controls.

## Controls

| Control | Action |
| --- | --- |
| Use the Eye in either hand | Select it and show its target and power status |
| Sneak + use while aiming at a player or mob | Select and bind it to a living visible target within the configured binding range |
| Swap hands while holding an Eye in the main hand (default F) | Select the held Eye and toggle Full / Off; the item stays in place |
| Drop while holding an Eye in the main hand (default Q, including Ctrl+Q) | Select the held Eye and toggle Normal / Off; the item is retained |
| Sneak + attack while holding an Eye in the main hand (default Shift + left click) | Select the held Eye and toggle Gaze; consume the attack or mining gesture |
| /hunterseye gaze | Toggle Hunter's Gaze for the selected carried Eye |
| /hunterseye phantom normal | Set Phantom Hunt to Normal |
| /hunterseye phantom full | Set Phantom Hunt to Full |
| /hunterseye phantom off | Turn Phantom Hunt off |
| /hunterseye status (or /hunterseye) | Show the selected Eye and power status |

The controls follow Minecraft's Swap Hands, Drop, Sneak, and Attack bindings, so players can rebind those vanilla actions. Item controls require the Eye in the main hand; use/bind also works in the offhand. Ordinary items retain vanilla controls. Powers are independent: turning Gaze off leaves Phantom Hunt unchanged. Normal and Full replace each other, and repeating the same item control turns that mode off. Commands explicitly set their mode; use off to end it.

An attack, block action, and its swing count as one Gaze gesture. Holding attack while mining does not repeatedly flip Gaze. Pause for five server ticks (about a quarter second at normal tick rate) before another deliberate Gaze gesture. Right-click binding and drop swings do not also activate Gaze.

Each Eye stores one target. Binding replaces that target. Selecting a different Eye disables the previous Eye's powers. Keep the selected Eye in the hotbar, ordinary inventory, or offhand while using other tools. Containers, bundles, armor slots, and ender storage do not count. Losing the item or dying disables powers; getting it back does not turn them on automatically. Duplicate identities from creative copying require a fresh selection.

Use is intercepted on blocks and entities as well as in the air. It selects/binds instead of trading, placing an eye in an End portal frame, consuming the Eye, or launching a vanilla stronghold search. Put the item away to interact with villagers or containers normally. Binding uses the server's gaze and line of sight, so walls, invisible targets, and Full-concealed players cannot be bound by ordinary sneak-use.

## Hunter's Gaze

The server updates the holder's action bar five times per second at normal tick rate. A live target shows its current coordinates and dimension, plus distance when in the same dimension. Restricted, offline, dead, removed, or unloaded targets show status without coordinates or a dimension. There is no stale-position fallback.

The regular Eye defaults to **128 blocks of tracking in the same dimension**. Administrators can change that distance or allow cross-dimension tracking. When cross-dimension tracking is enabled, any already-loaded target in the other dimension is permitted; no scalar distance is shown or applied between dimensions. Same-dimension targets still obey the configured range.

Only online players and already-loaded mobs can be tracked. The Eye never loads chunks or forces an offline player online. A target bound before it became invisible remains trackable if it is live and permitted.

Gaze sends the holder a private bounding box around an eligible same-dimension target, with glow visible through walls within the configured highlight range (default **64 blocks**). Twelve thin white concrete BlockDisplay rails follow the target's actual bounding box as it moves, changes player pose, or changes mob size. The rails are normally visible and non-pickable, with an exact configured RGB glow override. These client displays are sent only to that player, with no authoritative world entity or changes to the real target's glowing state. No resource pack is needed. Eligible already-bound Full-concealed targets can still have a private box. Vanilla display rendering limits still apply, especially at long range. Turning Gaze off or losing access removes the box.

## Admin settings

These commands require gamemaster permissions. Configuration is specific to the active world.

| Command | Allowed value | Default |
| --- | --- | --- |
| /hunterseye config show | Display current values | — |
| /hunterseye config bindRange blocks | Integer 1–128 | 32 |
| /hunterseye config trackingRange blocks | Integer 1–30000000 | 128 |
| /hunterseye config highlightRange blocks | Integer 1–512 | 64 |
| /hunterseye config normalDuration seconds | Integer 1–3600 | 120 |
| /hunterseye config fullDuration seconds | Integer 1–3600 | 30 |
| /hunterseye config normalCooldown seconds | Integer 0–3600 | 30 |
| /hunterseye config fullCooldown seconds | Integer 0–3600 | 30 |
| /hunterseye config crossDimension enabled | true or false | false |
| /hunterseye config color hex | Exactly six hexadecimal digits, without # | FF0000 |
| /hunterseye config reset | Restore and save all defaults | — |

Color changes the action-bar text, including tracking and ability status, and the bounding-box glow. Both use the exact RGB value, with no vanilla 16-color team restriction; the concrete rails remain white. Eye names remain violet. Highlight range and tracking range are separate limits; a box still requires a permitted live tracking snapshot. Range changes affect the next server snapshot. Save failures leave the live setting unchanged and report failure to the administrator.

## Phantom Hunt

Normal applies vanilla invisibility metadata: the body disappears while equipment remains visible. Full removes the player entity from other clients' entity tracking, hiding the body, equipment, nameplate, shadow, fire/glow, and attached render effects there. Revealing restores normal entity pairing. New observers and reconnecting Full players use the same server rules.

On vanilla clients, your own first-person hands, held item and F5 equipment can still render; the TAB player list still shows you. The optional [Eye Visuals 0.1.1](optional-client/README.md) Fabric client helper hides your own rendering while the server confirms Full. Install its jar only on clients, with Fabric API. It is unnecessary for gameplay or observer concealment and must not be installed on a dedicated server. Full hides the entity other clients normally aim at for direct melee interaction, so they cannot select it using their normal crosshair targeting. Server collision, non-movement sounds, dropped items, and ordinary world interaction remain. Full allows incoming damage, including area damage. This is concealment rather than removal from the world.

Full is a **noncombat mode**. Damage-dealing actions are blocked while it is active, and attempting an attack does not reveal you or end Full. Direct attacks, damaging item use, and damage attributed to your attacks/projectiles are checked on the server. Turn it off with /hunterseye phantom off before fighting. Normal remains ordinary invisibility and allows combat. Disabling an Eye mode never removes an unrelated invisibility potion.

Full also blocks common hazardous item actions (lava, ignition, TNT, crystals and explosive bed/anchor use). Existing offensive projectiles are neutralized; a recoverable survival trident is returned to its owner's inventory (or dropped by vanilla inventory handling if full), preserving its item data. Owned lingering-cloud applications pause. Already-applied status effects and unattributed redstone/environment chains retain vanilla behavior; this is not a world protection system.

Ordinary world interactions remain available while using other tools; holding the Eye intentionally intercepts use and its sneak-attack Gaze gesture as described above.

Full suppresses player movement particles and footsteps, including sprint dust, swimming/splashes, landing dust and Soul Speed trails. Potion gameplay continues while its body particles are hidden. Eye Visuals 0.1.1 also suppresses the owner's locally generated movement/body effects and attached particle emitters. Unrelated world particles and sounds remain; already emitted free particles finish their normal lifetime. Vanilla clients need the optional helper to suppress their own predicted movement effects locally.

## Durations and cooldowns

Effect durations are separate from cooldowns: Normal lasts **120 seconds**, Full **30 seconds** by default. The action bar shows the active mode's remaining time even with Gaze off. Expiry returns to Visible while preserving Gaze and cooldowns; unrelated potion effects remain. Timers use real time and continue across reconnects/restarts. Repeating an active-mode command never refreshes the timer. Changing settings affects future activations. Operators configure durations independently with `normalDuration` and `fullDuration` (1–3600 seconds).

Activation starts a real-time cooldown deadline: **Normal 30 seconds**, **Full 30 seconds** by default. Operators configure these independently with `normalCooldown` and `fullCooldown`; both accept 0–3600 seconds, and zero allows immediate reactivation. Changing settings affects future activations and preserves existing deadlines. Because cooldowns begin on activation, Full is normally ready to activate again when its 30-second effect expires.

Off is always allowed during recovery. A rejected activation keeps the current mode and both deadlines. Repeating the explicit command for the already-active mode does not charge again. `/hunterseye status` reports rounded-up remaining seconds for each mode. Cooldowns belong to each player, independently of Eye identity: swapping Eyes, losing one or dying cannot bypass them. Real time continues during reconnects and server shutdown. Restoring a saved active mode does not start another cooldown.

## Saved state and migration

Bindings are saved in the vanilla stack's namespaced custom data. Per-player selected UUID, Gaze, Phantom Hunt preferences and separate cooldown deadlines remain in the world's data/hunterseye-settings.json; the existing format is retained. Reconnecting/restarting restores powers only when the selected eligible Eye is still carried. Death disables powers while retaining the item binding and cooldown deadlines.

Administrative settings are stored separately in data/hunterseye-config.json. Changes use a temporary file and atomic replacement where supported. Invalid original files are preserved as .corrupt-UUID.bak before repair. Include both JSON files and inventories in world backups.

**Upgrade from 0.1.x:** back up the world before replacing the mod. The old custom hunterseye:hunters_eye registry item is no longer registered. Old custom stacks need replacement with /hunterseye give eye; they are not converted automatically, and their bindings are not copied. Existing player preferences remain saved, but a missing old selected item disables powers. Select and rebind the replacement. Remove the old Hunter's Eye client mod/resource setup when connecting to the new server. The optional owner-render helper uses a negotiated server confirmation; vanilla clients require no custom payload support.

**Upgrade from earlier 0.2.x:** existing tagged Clairvoyance items become regular Eyes when carried, preserving UUID and binding while replacing the name/model/lore. Remote operator binding, the alternate give command, range exemptions and incoming damage immunity are removed. Stored items migrate after retrieval. Missing configuration fields gain 30/30 cooldown and 120/30 duration defaults. Explicit settings remain unchanged; legacy active modes receive one bounded duration on migration.

## Build and verification

From this repository with Java 25:

    ./gradlew build

On Windows, use `.\gradlew.bat` in place of `./gradlew`. The root build produces the core jar and source jar under `build/libs`, builds the optional client helper under `optional-client/build/libs`, and packages the resource pack under `build/resourcepacks`. It runs `verifyPowers` and `verifyControls`, the included executable JVM harnesses, plus helper checks and pack selector validation.

The helper can also be built independently with `./gradlew -p optional-client build`; the pack can be packaged with `./gradlew resourcePack`. This repository contains the source and build inputs for all three components. See [ARTWORK.md](ARTWORK.md) for item art and [VERIFICATION.md](VERIFICATION.md) for coverage and gameplay limits.
