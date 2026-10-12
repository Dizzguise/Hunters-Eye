# Changelog

## 0.1.1

- Extended Eye binding and Hunter's Gaze from players to mobs, including villagers.
- Applied the existing visibility, line-of-sight, and 32-block binding checks to mobs.
- Made sneak-use bind villagers while preserving ordinary trading interaction.
- Added loaded-mob tracking across dimensions and Unavailable status for unloaded or removed mobs without forcing chunks to load.
- Preserved compatibility with existing player bindings.

## 0.1.0

- Initial standalone release for Minecraft 26.2 and Fabric.
- Added the Hunter's Eye item with per-item player binding and selection.
- Added Hunter's Gaze coordinate tracking and a private projected target marker.
- Added Phantom Hunt Normal and Full invisibility, rebindable J/K/L controls, and HUD status.
- Added per-player, per-world saved selection and powers, plus carried-item and death handling.
