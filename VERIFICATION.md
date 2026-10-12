# Hunter's Eye verification

Release: **0.1.1**, Minecraft **26.2**, Java **25**, Fabric Loader **0.19.3**, Fabric API **0.161.0+26.2**.

## Checks included in this repository

From the repository root, run:

```powershell
.\gradlew.bat build sourcesJar
```

`build` runs `verifyPowers`, an executable JVM harness with **66 checks**. The harness covers mode transitions, saved identity, corrupt-file recovery, save retries, and packet round trips. These checks passed for the release. Deliberately corrupt fixtures produce expected error logs during a passing run. The harness can also be run directly with `.\gradlew.bat verifyPowers`.

## External integration coverage

A separate development integration suite passed **22 combined server GameTests** against Hunter's Eye and Enoch. Hunter's Eye coverage included mob raycasts, line of sight, the 32-block binding range, unloaded and cross-dimension targets, legacy player bindings, and villager trading behavior. This integration test source is not included in this repository; the build command above runs the included powers harness only. The combined count is not a count of Hunter's Eye-only tests.

## Manual gameplay status

Native Minecraft client checks passed for binding a villager and using J, and for L, E, and mouse interaction. The tester reported that the player/mob tracking change appears to work; deeper gameplay testing is still pending.

These checks do not establish complete rendered visual correctness. In particular, Full invisibility still needs observation from a second client for equipment, nameplate, shadows, fire/glow, and attached particles. The private target marker needs visual checks through walls, with invisible targets, and when the target model is not loaded.

## Remaining acceptance checks

- Bind players and several mob types, including villagers; confirm sneak-use binds and ordinary villager interaction still trades.
- Track a moving target in the same dimension and across dimensions. Unload or remove a mob and confirm Unavailable replaces its coordinates.
- Switch Eyes, move the selected Eye out of carried inventory, reconnect, restart, and die; confirm selection and power behavior matches the README.
- Check J/K/L rebinding and conflicting game controls with and without a selected Eye.
- Observe Normal and Full from a second client and confirm Hunter's Gaze remains private to its holder.
- Exercise each listed Full-reveal action and verify unrelated invisibility potions survive disabling a mode.
- Repeat multiplayer checks on a dedicated server with two clients and inspect the server/client logs for errors.
