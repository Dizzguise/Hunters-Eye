# Eye Visuals 0.1.1

Optional **Fabric client helper for Minecraft Java 26.2**, used with Hunter's Eye 0.2.6 on the server. Install `eye-visuals-0.1.1.jar` and Fabric API 0.161.0+26.2 on a client running Fabric Loader 0.19.3+ and Java 25. For single-player, put it in the local Fabric instance alongside Hunter's Eye. Do not install it on a dedicated server.

While the server confirms that your own Phantom Hunt mode is Full, the helper hides your local player rendering: body, armor, held equipment, cape/name, fire and shadow in F5, plus first-person hands and held items. It adds no controls or powers. Keep using the server's existing Eye gestures and `/hunterseye phantom normal|full|off` commands. Normal and Off retain their normal rendering.

The state is scoped to the current connection/world/player and expires if the server stops confirming Full. Off, death, item loss, disconnect and world changes clear or invalidate it. The helper does not hide other entities, modify incoming damage, alter cooldowns or change the server's observer concealment. Vanilla clients can use all server gameplay without it; their own hands/equipment may remain visible locally.

The optional 0.2.4 resource pack provides item artwork separately; it is unnecessary for this rendering helper.

Build with Java 25 using `gradlew.bat build`. The installable jar is in `build/libs`; the source jar is for inspection. See [the repository verification report](../VERIFICATION.md) for recorded checks and remaining gameplay coverage.

Full suppresses player movement particles and footsteps, including sprint dust, swimming/splashes, landing dust and Soul Speed trails. Potion gameplay continues while its body particles are hidden. Eye Visuals 0.1.1 also suppresses the owner's locally generated movement/body effects and attached particle emitters. Unrelated world particles and sounds remain; already emitted free particles finish their normal lifetime. Vanilla clients need the optional helper to suppress their own predicted movement effects locally.
