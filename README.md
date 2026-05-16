# Atomics Client

A Fabric **Minecraft 1.21.11 / Java 21** client mod scaffold for insanely customizable totem pop visuals.

## Features included

- Detects vanilla totem pops from the client status packet.
- Configurable particle bursts with random, sphere, ring, spiral, beam, and cone shapes.
- Configurable sounds with per-sound tick delays for sequencing.
- Optional command-based temporary entities.
- Custom totem item texture/model override.
- Configurable totem scale in hand.
- Attempted configurable totem pop overlay scale.

## Build target

Fabric's 1.21.11 migration note says 1.21.11 should use Loom 1.14 and Fabric Loader 0.18.1+.
This project uses:

```properties
minecraft_version=1.21.11
yarn_mappings=1.21.11+build.1
loader_version=0.18.1
fabric_version=0.140.0+1.21.11
```

If Gradle says one of those dependency versions does not exist, open `gradle.properties` and replace it with the exact newest version from Fabric's version pages.

## How to build

1. Open the folder in IntelliJ IDEA.
2. Let Gradle import.
3. Run:

```bash
./gradlew build
```

On Windows:

```powershell
gradlew.bat build
```

The jar will be in:

```txt
build/libs/
```

## Config file

After first launch, edit:

```txt
.minecraft/config/atomics_client.json
```

Example things you can change:

```json
{
  "enabled": true,
  "particles": {
    "enabled": true,
    "bursts": [
      {
        "particle": "minecraft:totem_of_undying",
        "count": 80,
        "shape": "random",
        "spreadX": 0.65,
        "spreadY": 0.95,
        "spreadZ": 0.65,
        "speed": 0.12
      }
    ]
  },
  "temporaryEntities": {
    "enabled": false,
    "commands": [
      {
        "command": "summon minecraft:item_display ~ ~1.2 ~ {Tags:[\"atomics_client_tmp\"],item:{id:\"minecraft:totem_of_undying\",count:1}}",
        "aliveTicks": 40
      }
    ]
  },
  "item": {
    "handScaleEnabled": true,
    "handScale": 1.35
  },
  "popOverlay": {
    "scaleEnabled": true,
    "popScale": 1.75
  }
}
```

## Important note about temporary entities

The included temporary entity system is command-based because this is a client visual mod.
That means it needs singleplayer cheats or server permission to run `/summon` and `/kill` commands.
If you want this to work on public servers with no permissions, the better design is rendering fake client-only visuals instead of real entities.

## Files to edit most often

- `src/main/resources/assets/atomics_client/textures/item/custom_totem.png`
- `src/main/java/com/atomics/atomicsclient/config/TpsConfig.java`
- `.minecraft/config/atomics_client.json`

## Mixin warning

Minecraft 1.21.11 is still obfuscated, and exact Yarn method names can shift by build.
If the mod fails at runtime, check these mixins first:

- `ClientPlayNetworkHandlerMixin` — totem pop packet detection
- `HeldItemRendererMixin` — in-hand totem scaling
- `GameRendererMixin` — pop overlay scale

The particle/sound config is the most stable part. The overlay and hand scaling are the most mapping-sensitive parts.

## GUI Studio Update

Press **O** in-game to open Atomics Client. You can change the key in:

Options → Controls → Atomics Client → Open Atomics Client

The screen lets you edit:
- enable/disable the mod
- particle id, shape, count, spread, and speed
- sound id, volume, pitch, and delay
- temporary entity command and lifetime
- hand scale
- pop overlay scale and animation ticks
- live preview toggle

Click **Preview Now** to test the effect on yourself. Click **Save** to write changes to `config/atomics_client.json`.

Temporary entity commands require command permission/cheats because the client sends the command as your player.
