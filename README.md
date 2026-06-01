# Atomics Client

A Forge **Minecraft 1.20.1 / Java 17** client mod with configurable PvP quality-of-life features and totem-pop visuals.

## Features

- Configurable totem-pop particles and sounds.
- Totem item replacement and held-item scaling.
- Zoom, freelook, full-bright, time changer, projectile trails, and streamer mode.
- PvP session stats, opponent tags, reach display, friend/foe lists, and dual-spectate camera.
- Armor HUD, durability warnings, food preview bars, and partial-heart markers.
- In-game configuration studio. Press **O** by default.

## Build Target

This branch uses:

```properties
minecraft_version=1.20.1
forge_version=47.4.20
mapping_channel=official
mapping_version=1.20.1
```

Use Java 17 or newer to run Gradle. ForgeGradle compiles the mod for Java 17.

## Build

On Windows:

```powershell
.\gradlew.bat build
```

On macOS or Linux:

```bash
./gradlew build
```

The jar is written to `build/libs/`.

## Config

The client writes its configuration to:

```txt
.minecraft/config/atomics_client.json
```

Use the in-game studio for normal editing. Particle and sound list editors are available from the studio.

## Notes

Atomics Client is client-side only. Features that send chat commands still require the permissions that the connected server normally enforces.
