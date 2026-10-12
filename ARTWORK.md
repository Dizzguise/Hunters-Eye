# Item artwork

Hunter's Eye 0.2.5 uses a tagged vanilla Ender Eye and the `hunterseye:hunters_eye` string at custom-model-data index 0. Without a resource pack it has the vanilla appearance. The optional 0.2.4 resource pack supplies the original violet-and-gold artwork. No art is regenerated or recolored, and ordinary Ender Eyes retain their vanilla fallback.

The [pack source and installation guide](resource-pack/README.md) are included in this repository. Source assets live under `resource-pack/pack`. Run `./gradlew resourcePack` to produce `build/resourcepacks/hunters-eye-resource-pack-26.2-0.2.4.zip`; the root `build` task also packages it.

Legacy tagged Clairvoyance items migrate to the regular Eye selector/name/lore while preserving identity and binding. The removed variant has no artwork asset, pack selector or powers.

The pack supports unmodified Minecraft 26.2 clients and is separate from optional [Eye Visuals 0.1.1](optional-client/README.md). That client helper controls the owner's rendering, particles and footsteps during Full; it neither replaces item art nor grants powers. Server pack delivery uses an HTTPS URL, SHA-1 and require-resource-pack setting. The core jar has no client renderer or custom item registry.
