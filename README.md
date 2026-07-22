# FrostBear

FrostBear is a Fabric mod for Minecraft Java Edition 26.2 that adds a powerful frozen polar-bear boss, new combat encounters, custom loot, and an ice-spike weapon.

## Highlights

- A 260-health FrostBear boss with a boss bar, glowing eyes, custom sounds, and heavy knockback resistance.
- Roar, frost breath, charging, chilling melee attacks, and ground-based ice-spike waves.
- Natural encounters in snowy and icy biomes.
- A dedicated FrostBear guardian near igloos.
- Polar bears transform into FrostBears when affected by both Slowness and Weakness.
- A custom model with a reworked head, visible jaw, connected back crystals, and dedicated animations.

## Rewards

### Frozen Heart

Right-click for instant Heart of Winter activation—there is no eating animation. The effect grants freeze immunity, slows and freezes nearby hostile mobs, and provides a short regeneration burst.

### Glacial Core

A FrostBear crafting material used to create and repair Frostfang.

### Frostfang

A diamond-tier sword that chills enemies. Right-click to erupt a forward wave of ice spikes, followed by a six-second cooldown.

Crafting recipe:

```text
Glacial Core
Blue Ice
Stick
```

## Requirements

| Component | Version |
| --- | --- |
| Minecraft Java Edition | 26.2 |
| Mod loader | Fabric |
| Fabric Loader | 0.19.3 or newer |
| Fabric API | Required |
| Java | 25 |

The mod runs on both integrated and dedicated servers. Clients connecting to a modded server also need FrostBear and Fabric API installed.

## Installation

1. Install Fabric Loader for Minecraft 26.2.
2. Install Fabric API for Minecraft 26.2.
3. Download `frostbear-26.2-1.1.0.jar` from the GitHub Releases page, Modrinth, or CurseForge.
4. Place the JAR in the profile's `mods` directory.
5. Start Minecraft with the corresponding Fabric profile.

## Finding the FrostBear

- Search snowy and icy biomes for natural spawns.
- Explore igloos for a guardian encounter.
- Apply both Slowness and Weakness to a polar bear.
- Use the creative-mode spawn egg.
- Run `/summon frostbear:frost_bear ~ ~ ~`.

Useful command:

```mcfunction
/give @s frostbear:frost_bear_spawn_egg
```

## Screenshots

Release screenshots belong in [`screenshots/`](screenshots/). The folder contains the recommended filenames and capture guidelines.

## Building from source

Install JDK 25, clone the repository, and run:

```powershell
.\gradlew.bat build
```

On macOS or Linux:

```bash
./gradlew build
```

The distributable JAR is generated at:

```text
build/libs/frostbear-26.2-1.1.0.jar
```

Launch an isolated development client with:

```powershell
.\gradlew.bat runClient
```

Launch the dedicated development server with:

```powershell
.\gradlew.bat runServer
```

## Development model portal

The model portal is maintained as a separate local development tool and is not part of the published mod source tree. The Java model and production textures used by Minecraft are under `src/main`.

## License

FrostBear is available under the [MIT License](LICENSE).
