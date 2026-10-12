# Verification — Hunter's Eye 0.2.5

Target environment: Minecraft Java 26.2, Java 25, Fabric Loader 0.19.3 and Fabric API 0.161.0+26.2. Components in this repository are Hunter's Eye 0.2.5, optional Eye Visuals 0.1.1 and the original artwork resource pack 0.2.4.

## Reproducible repository checks

The independent repository build passed on October 12, 2026 with Java 25. Run `./gradlew build` (`.\gradlew.bat build` on Windows). The core includes 261 executable JVM checks: 138 in `verifyPowers` and 123 in `verifyControls`. These cover duration/cooldown defaults and boundaries, absolute deadlines, legacy migration, persistent configuration/player state, control routing, idempotence, rollback, bounded retry and interruption handling. They run without a sibling project or game installation.

The root build also passed the optional client helper's state freshness and protocol initialization/round-trip checks, plus resource-pack selector validation. The helper can be built independently with `./gradlew -p optional-client build`. These checks do not replace gameplay or rendering tests. The rebuilt core and helper jars match the previously tested 0.2.5 and 0.1.1 release jars byte for byte.

## Historical release integration evidence

The following release 0.2.5 results were recorded on October 12, 2026 in the external integration workspace. Its versioned XML/logs, native reports/screenshots and release hashes remain external evidence; they are not bundled here or rerun merely by building this repository. They apply to the identical core and helper jars produced by the current build.

- The final core jar passed 46 tests in the combined integration server suite and 3 Hunter's Eye-only server tests. Eye-related packet checks covered ticking countdowns, preserved Gaze, visibility on expiry, reconnect/offline expiration, server footstep/swim/landing suppression and potion metadata restoration while real effect durations continued. No storage errors appeared in the final server logs.
- A real client with Eye Visuals passed render-submission checks, Normal/Full/Visible movement particle and sound counters, owner and unpaired-remote particle-emitter termination, one-second effect expiry restoring visibility, item retention, original art, native/rebound controls and private red boxes.
- A real client without the helper passed native controls, remote concealment, art, tracking and box lifecycle checks. The core mixins loaded without the helper.
- Transient Windows access-denied errors during rapid atomic configuration replacement led to a bounded retry of that failure after 10/25/50 ms. The final release suites passed after that change; atomic replacement, original data, dirty state/rollback and interruption behavior remained covered.

Normal/Full durations default to 120/30 seconds and accept 1–3600; cooldowns default to 30/30 and accept 0–3600, beginning on activation. Expiry ends concealment without stopping Gaze or changing independent cooldown deadlines. Configuration changes affect subsequent activations. Full permits incoming damage and ordinary world interaction with other tools; the held Eye intentionally consumes use and the sneak-attack Gaze gesture.

Particle suppression is attributed to a player at the emission source and preserves potion gameplay, damage, sculk events and unrelated world effects. Already emitted free particles expire naturally. Owner-predicted effects require the optional client helper.

Independent dedicated-server playtesting with two real clients remains user acceptance work. The JVM harnesses and historical integration reports cannot establish every multiplayer interaction or vanilla rendering behavior.
