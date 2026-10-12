# Hunter's Eye artwork pack

This optional Minecraft Java 26.2 pack restores the original violet-and-gold Eye artwork. It changes only tagged Hunter's Eyes; ordinary Ender Eyes retain their vanilla model. The approved texture is kept in `pack/assets/hunterseye/textures/item/hunters_eye.png`.

From the repository root, run `./gradlew resourcePack` (`.\gradlew.bat resourcePack` on Windows) with Java 25. The pack is also included in `build` and is written to `build/resourcepacks/hunters-eye-resource-pack-26.2-0.2.4.zip`. No other repository, game installation, or backup folder is needed to package these sources. Pack version 0.2.4 is independent of the server mod version.

Players can place that ZIP in Minecraft's `resourcepacks` folder and enable it. A server can host the ZIP at a direct HTTPS download URL and set these values in `server.properties`:

```properties
resource-pack=https://your-host.example/hunters-eye-resource-pack-26.2-0.2.4.zip
resource-pack-sha1=SHA1_OF_THE_EXACT_HOSTED_ZIP
require-resource-pack=true
```

Replace the example URL and digest. Obtain the digest with `Get-FileHash -Algorithm SHA1 <zip>` in PowerShell or `sha1sum <zip>` on Linux. Change `require-resource-pack` to `false` if players may decline. The pack requires no client mod and is separate from the optional Eye Visuals helper.
