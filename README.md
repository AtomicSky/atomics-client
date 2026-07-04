# Legions Client

Fabric client-side team coordination tools for the Legions Minecraft server.

## Features

- Legions-aware roster HUD with all teammates and a configurable count of nearest opponents.
- Rating suffixes on player nametags when ratings are visible in the player list.
- Optional foe/spectator outlines and warning particles.
- Crosshair ping keybind that marks the block or player you are looking at.
- Same-team clients can read the ping message and show particles around the marked block/player.
- Mod Menu config screen.

## Build

```powershell
gradlew.bat build
```

The jar is written to:

```txt
build/libs/legions-client.jar
```

## Notes

The ping feature sends a normal chat message by default. If Legions has a dedicated team-chat command, route pings through that command before using it in matches where global chat is visible.
