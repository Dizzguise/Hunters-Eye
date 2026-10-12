# Changelog

## 0.2.7

- Revise the Eye recipe to diamond-block corners, a netherite spear at top, a netherite sword at left, a netherite axe at right, a beacon at bottom, and an Ender Eye center. Each craft consumes four diamond blocks and one of each other ingredient.
- Verify the new grid, vanilla horizontal mirroring, and rejection of the former gold-block recipe. Keep the recipe-book unlock, output components, powers, Eye Visuals 0.1.1, and artwork pack 0.2.4 unchanged.

## 0.2.6

- Add a Survival crafting-table recipe using one Ender Eye, four gold blocks, a diamond sword, a diamond axe, and two netherite ingots. Crafting creates an unbound Eye with its own identity.
- Unlock the recipe when acquiring an Ender Eye. The vanilla recipe-book result retains the custom name and original artwork selector; no client mod is required.
- Restrict every Hunter's Eye command, including status and ability commands, to gamemaster operators or players currently in Creative mode. Survival item controls are unchanged, and command suggestions refresh on game-mode changes.
- Add 47 vanilla recipe/advancement/output/display checks. Keep optional Eye Visuals 0.1.1 and artwork pack 0.2.4 unchanged.

## 0.2.5

- Set both default activation cooldowns to 30 seconds; keep independent configurable deadlines.
- Retry transient Windows configuration/settings replacement locks for at most 85ms without deleting the previous file or weakening rollback.
- Add persisted real-time effect durations: Normal 120 seconds and Full 30 seconds, configurable by operators from 1 to 3600 seconds.
- Show the active countdown with Gaze on or off; expiry preserves Gaze, potion gameplay and cooldown deadlines.
- Suppress Full movement sounds and body particles at their sources; optional Eye Visuals 0.1.1 covers owner-predicted effects and stale attached emitters.

## 0.2.4

- Add independent player-owned real-time activation cooldowns: Normal 120 seconds and Full 30 seconds by default, with operator settings 0–3600, show/reset and persistent transactional configuration.
- Retain deadlines across swaps, item loss, death, reconnects and restarts; always permit Off, preserve the current mode on blocked activation and avoid recharging repeated explicit commands or restored active modes.
- Remove Clairvoyance, remote administrative binding, tracking/highlight exemptions and incoming damage immunity. Legacy tagged items migrate to ordinary Eyes while retaining identity and binding. Full remains noncombat and vulnerable to incoming damage.
- Support the optional Eye Visuals 0.1.0 client helper for the owner's F5 equipment/body and first-person hands, while retaining vanilla-client gameplay and observer concealment.
- Update original artwork packs to 0.2.4 with one regular Eye selector and unchanged approved art.

## 0.2.3

- Reduced bounding-box rails from 0.03 to 0.015 blocks and changed the default Gaze color to red (FF0000). Existing explicit world color settings remain configurable.

## 0.2.2

- Replaced the overhead cube marker with a private bounding box made from twelve thin white concrete BlockDisplay rails. Rails follow actual target bounds, including player pose, mob size, movement, and teleportation.
- Use the display glow-color override for exact configured RGB through-wall glow and action-bar text, removing the nearest-team-color restriction. The rails remain white in normal view.
- Keep displays non-pickable and private to the holder, without authoritative world entities, global target glow changes, a helper client, or a resource pack dependency for the box.
- Preserve existing binding, tracking, Full-target eligibility, same-dimension highlight rules, and Clairvoyance range bypass. Vanilla display rendering limits still apply.
- Keep original artwork packs at 0.2.1. No crafting recipes or gameplay rules change in this display update.

## 0.2.1

- Added vanilla main-hand Eye controls: Swap Hands toggles Full, Drop (including Ctrl+Drop) toggles Normal, and Sneak + Attack toggles Gaze. Controls select the held Eye and preserve the item; ordinary items keep vanilla behavior.
- Consume sneak attacks and block-mining gestures before combat handling, including while Full is active. Restore predicted drops/block changes and collapse related packets and held mining into one Gaze gesture.
- Suppress binding/drop follow-up swings so they cannot activate Gaze accidentally. Controls use the same server permission and state transitions as commands.
- Added a private vanilla outline cube above the tracked target, sent only to the authorized holder. No world entity, global glowing changes, client helper, or custom network payload is used.
- Added independently persisted highlightRange (default 64, operator bounds 1–512) and nearest vanilla outline color selection. Clairvoyance bypasses ordinary same-dimension highlight range; vanilla render-distance limits apply.
- Added optional original artwork through resource-pack model selection on tagged vanilla Eyes, preserving server-only functionality.
- Added 120 gesture/configuration regression checks alongside the existing 75 power checks, including held mining, packet deduplication, use/drop swing suppression, old config migration, and failed-save rollback.

## 0.2.0

- Converted Hunter's Eye to a server-only vanilla item and command flow; unmodified clients need no mod or resource pack.
- Replaced the custom registered item with tagged vanilla Ender Eyes with unique identity, custom names/lore, and a one-item stack limit.
- Replaced client J/K/L controls, HUD, marker, and custom payloads with /hunterseye gaze, /hunterseye phantom normal|full|off, status, and five-Hz private action-bar tracking.
- Added world-saved operator settings for binding range, tracking range, cross-dimension tracking, and text color, with transactional persistence.
- Added operator-only Clairvoyance and held-item admin binding. Re-check permissions on use and every tick; no chunk forcing or offline-player loading.
- Enforced normal Eye range/dimension limits before producing tracking coordinates.
- Implemented Normal using native invisibility metadata and Full using native server entity pairing for other observers, including late pairing and reconnect restoration.
- Made Full a noncombat mode: offensive actions are blocked without ending concealment. Authorized carried Clairvoyance grants incoming-damage immunity only during Full; Off restores vulnerability. No flight or game-mode changes.
- Kept power preferences and potion invisibility independent. Existing 0.1.x custom items need explicitly minted replacements; no automatic conversion is performed.
- Removed the obsolete custom texture and client code. Own first-person hands and TAB remain visible on vanilla clients.

## 0.1.1

- Extended Eye binding and Gaze from players to loaded mobs with visibility and line-of-sight checks.
- Preserved compatibility with older player bindings.

## 0.1.0

- Initial standalone client/server release with a custom Eye, ability keys, HUD/marker, saved selection, and independent Gaze and Phantom Hunt powers.
