# Hunter's Eye

An independent **Minecraft Java 26.2 / Fabric** mod. The violet-and-gold **Hunter's Eye** grants **Hunter's Gaze** (tracking) and **Phantom Hunt** (invisibility). Install it on the server and all participating clients, or use it in single-player.

## Install and obtain

Download the installable jar from [Hunter's Eye releases](https://github.com/Dizzguise/Hunters-Eye/releases).

Use Java 25, Fabric Loader 0.19.3 or newer, and Fabric API **0.161.0+26.2**. Copy `hunters-eye-0.1.1.jar` and the matching Fabric API jar to the instance's `mods` folder. No Enoch or Ghost Slots dependency is required.

Find the Eye in Creative **Tools & Utilities**, or run `/give @s hunterseye:hunters_eye`. No crafting recipe is included yet.

## Controls

| Control | Action |
| --- | --- |
| Sneak + Use while aiming at a player or mob | Bind this Eye to a visible player or mob within 32 blocks; replaces its old target |
| Use | Select this Eye and show its target/status |
| J | Toggle Hunter's Gaze |
| K | Toggle Phantom Hunt: Normal |
| L | Toggle Phantom Hunt: Full |

J/K/L are rebindable under Options > Controls > Key Binds > Hunter's Eye. Each Eye remembers one target. Binding selects the Eye; selecting a different Eye turns existing powers off. Keep the selected Eye in your hotbar, ordinary inventory, or offhand to maintain powers while using other tools. Containers, bundles, armor slots, and ender storage do not count as carrying it. Losing it disables powers; getting it back does not automatically enable them. Ambiguous Creative duplicates require a fresh selection.

## Hunter's Gaze

Ability keys take priority over conflicting game bindings while the selected Eye is carried. This includes the default L/Advancements conflict. Without an active Eye, ordinary game bindings remain available.

The upper-left HUD displays the bound target's current coordinates and dimension, with same-dimension distance. Updates arrive five times per second at normal server tick rate. Players remain trackable across dimensions while online. Mobs remain trackable across dimensions while loaded; unloaded or removed mobs show Unavailable instead of stale coordinates. Tracking does not load chunks. Old player bindings remain compatible; interrupted updates display Waiting for server.

Within 64 blocks in the same dimension, a gold outline sized to the loaded target marker appears through walls. This is a private projected marker, not a glowing effect shared with other players. It works through either invisibility mode and uses server coordinates if the target's model is not loaded. Targets behind the camera or outside the view are not drawn. No distant chunks are force-loaded for tracking.

## Phantom Hunt

Visible, Normal, and Full are mutually exclusive. Normal uses vanilla-style invisibility: body hidden, equipment visible. Full suppresses the player's complete model submission, including armor, held items, cape, nameplate, fire overlay, shadow, and glow, plus attached potion/emitter/consumption particles and sprint dust. Your first-person hands/items also disappear in Full. The HUD indicates the active mode.

Full ends on an entity attack attempt (even if damage is blocked), stabbing, riptide, firing a bow/crossbow, throwing a trident, and attack projectiles. All thrown splash/lingering potions reveal, including helpful ones; drinking potions does not. Mining and empty swings do not reveal. Normal does not break on attack. Disabling a mode never removes an unrelated invisibility potion.

This is visual concealment. Collision, sounds, damage, dropped items, fired projectiles, and world interaction remain. Hunter's Gaze still reveals a bound hidden player to its holder.

## Saved state

Bindings live on each item's custom data. Selection, tracking, and Phantom Hunt state are stored per player UUID under the world's `data/hunterseye-settings.json`. Reconnect/restart restores enabled powers only when that Eye is still carried. Death turns powers off while keeping item bindings. Worlds have independent state.

Changes save immediately using a temporary file and atomic replacement where supported. Failed saves retry. Invalid settings are backed up as `.corrupt-<UUID>.bak` before repair. Include these files in normal world backups.

## Build and test

Run the following from this repository's root with Java 25:

```powershell
.\gradlew.bat build sourcesJar
```

This repository builds independently. Installable and source jars are in `build/libs`. The `verifyPowers` task runs 66 executable JVM checks, including mode transitions, saved identity, corrupt-file recovery, save retries, and packet round trips. Deliberately corrupt fixtures produce expected error logs during a passing test run. See [VERIFICATION.md](VERIFICATION.md) for tested coverage and remaining gameplay checks, and [CHANGELOG.md](CHANGELOG.md) for release changes.
