# Verification — Hunter's Eye 0.2.6

Target environment: Minecraft Java 26.2, Java 25, Fabric Loader 0.19.3 and Fabric API 0.161.0+26.2. Components in this repository are Hunter's Eye 0.2.6, optional Eye Visuals 0.1.1 and the original artwork resource pack 0.2.4.

## Reproducible repository checks

The independent repository build passed on October 12, 2026 with Java 25. Run `./gradlew build` (`.\gradlew.bat build` on Windows). The core includes 308 executable JVM checks: 138 in `verifyPowers`, 123 in `verifyControls`, and 47 in `verifyRecipe`. These cover duration/cooldown defaults and boundaries, absolute deadlines, legacy migration, persistent configuration/player state, control routing, idempotence, rollback, bounded retry and interruption handling. They run without a sibling project or game installation.

The root build also passed the optional client helper's state freshness and protocol initialization/round-trip checks, plus resource-pack selector validation. The helper can be built independently with `./gradlew -p optional-client build`. These checks do not replace gameplay or rendering tests. The optional helper is unchanged at 0.1.1; the core is 0.2.6. Recipe checks decode vanilla recipe and advancement data, validate every ingredient, compare crafted and recipe-book result components, and ensure separate outputs receive separate identities.

## Current crafting and command integration

The 0.2.6 core with Enoch 0.2.3 passed all 51 combined Minecraft server GameTests on October 12, 2026. New tests use actual recipe-manager entries, crafting-menu result clicks, ingredient consumption, inventory-triggered book unlocks, tagged display components, unique crafted Eye identities and Survival item controls. Permission tests use ordinary ServerPlayers and cover non-operator Survival/Adventure/Spectator rejection, Creative and operator access, transitions back to Survival, and command-tree update packets. Existing gameplay regressions also passed, and the isolated Hunter's Eye suite passed 3/3 cases. These external integration harnesses are separate from the repository's portable JVM checks.

A native Minecraft client without the optional helper also passed recipe-book verification against both current core jars. Acquiring ingredients unlocked both recipes; the actual buttons retained custom names/lore and resolved the original artwork sprites with the pack enabled. Native mouse clicks auto-filled all nine slots and crafted each output. Screenshots were visually checked. The external client harness and captures are not part of the portable build.

## Historical release integration evidence

The following release 0.2.5 results were recorded on October 12, 2026 in the external integration workspace. Its versioned XML/logs, native reports/screenshots and release hashes remain external evidence; they are not bundled here or rerun merely by building this repository. These describe the prior core release and unchanged helper; current 0.2.6 validation is recorded separately below.

- The final core jar passed 46 tests in the combined integration server suite and 3 Hunter's Eye-only server tests. Eye-related packet checks covered ticking countdowns, preserved Gaze, visibility on expiry, reconnect/offline expiration, server footstep/swim/landing suppression and potion metadata restoration while real effect durations continued. No storage errors appeared in the final server logs.
- A real client with Eye Visuals passed render-submission checks, Normal/Full/Visible movement particle and sound counters, owner and unpaired-remote particle-emitter termination, one-second effect expiry restoring visibility, item retention, original art, native/rebound controls and private red boxes.
- A real client without the helper passed native controls, remote concealment, art, tracking and box lifecycle checks. The core mixins loaded without the helper.
- Transient Windows access-denied errors during rapid atomic configuration replacement led to a bounded retry of that failure after 10/25/50 ms. The final release suites passed after that change; atomic replacement, original data, dirty state/rollback and interruption behavior remained covered.

Normal/Full durations default to 120/30 seconds and accept 1–3600; cooldowns default to 30/30 and accept 0–3600, beginning on activation. Expiry ends concealment without stopping Gaze or changing independent cooldown deadlines. Configuration changes affect subsequent activations. Full permits incoming damage and ordinary world interaction with other tools; the held Eye intentionally consumes use and the sneak-attack Gaze gesture.

Particle suppression is attributed to a player at the emission source and preserves potion gameplay, damage, sculk events and unrelated world effects. Already emitted free particles expire naturally. Owner-predicted effects require the optional client helper.

Independent dedicated-server playtesting with two real clients remains user acceptance work. The JVM harnesses and historical integration reports cannot establish every multiplayer interaction or vanilla rendering behavior.
