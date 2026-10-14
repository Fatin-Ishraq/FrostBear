# FrostBear

![FrostBear banner](screenshots/frostbear-banner.png)

FrostBear is a Fabric and NeoForge mod for Minecraft Java Edition 26.2 that adds a powerful frozen polar-bear boss, new combat encounters, custom loot, and an ice-spike weapon.

## Highlights

- A two-phase boss fight where every attack has a readable tell, a counter, and a punish window.
- Frostbite: a stacking cold that slows, stops sprinting, and Shatters at five stacks. Warmth melts it.
- Positional combat: an armoured front, open flanks, and fire that melts its guard.
- No safe spots: it answers pillars, pits, and walls, and resets if you leave.
- Natural encounters in snowy and icy biomes, and a dedicated guardian near igloos.
- Polar bears transform into FrostBears when affected by both Slowness and Weakness.
- A crystal-crowned model with an expressive face: it blinks, scowls, sniffs, and roars.

## The fight

The FrostBear has 500 health, 10 armour, and a boss bar. A fight takes about three to five minutes for one player in diamond gear.

### Phase 1 (full to half health)

| Move | Tell | Counter | Punish |
| --- | --- | --- | --- |
| **Maul** (swipe, swipe, bite) | Raises a paw | Back off, or circle behind it; the bite breaks shields | Short recovery after the bite |
| **Charge** | Scrapes the ground, head lowered | Sidestep: the direction locks when it starts running | Crashing into a solid wall stuns it for 2.5 s |
| **Frost Breath** | Inhales; frost streams into its jaw | Leave the sweep, or get behind it | Rooted while breathing; its flanks are open |
| **Ground Slam** | Rears up | Step off the spike line; each spike is marked with frost first | Long recovery |
| **Roar** | Rears up and bellows | Break line of sight or get out of range | — |

### Phase 2: Heart of Winter (half health to zero)

At half health it roars (and cannot be hurt during the roar), its ice cracks, its crystals blaze, and the boss bar turns purple. It moves faster and recovers sooner, and adds:

| Move | Tell | Counter |
| --- | --- | --- |
| **Ice Ring** | Hunches while frost rises around it | Back off before the rings burst |
| **Frost Leap** | Crouches while a frost ring tracks you; the ring freezes in place when it jumps | Get out of the ring before it lands |
| **Glacial Cataclysm** | Plants itself for 3 s; the sky darkens, the boss bar turns white, and a chime climbs | Hide behind solid blocks, or deal 30 damage (or any fire) to interrupt it and stun it for 3.5 s |

The Cataclysm is never used on Easy. On Hard, every cooldown is shorter.

### Rules

- **Frontal guard:** hits from in front of it deal 40% less damage; hits from the sides and back deal full damage. Glancing hits clink off its ice.
- **Fire:** the bear never burns, but Fire Aspect, Flame arrows, fire charges, and lava melt its guard for five seconds.
- **Stun:** while stunned it has no guard and takes double damage.
- **No safe spots:** if it cannot reach you for three seconds, it erupts ice from the block you are standing on, or leaps at you in phase 2. With the `mobGriefing` game rule on, its slams and landings shatter soft blocks (snow, ice, dirt, sand, leaves; the `frostbear:frost_bear_breakable` block tag).
- **Leash:** if no player is within 32 blocks for 15 seconds, it heals fully and resets to phase 1.
- **Groups:** each extra player nearby (up to four) adds half its base health.
- **Resistances:** Slowness and Weakness last half as long on it.

## Frostbite

Every FrostBear attack, ice spike, and frost weapon adds Frostbite stacks (up to five, refreshed on each hit):

| Stacks | Effect |
| --- | --- |
| 1–4 | 8% slower per stack, with frost creeping over the screen |
| 3+ | Cannot sprint |
| 5 | **Shatter**: 7 damage (credited to the attacker), Frostbite clears, and the target gains **Thaw** |

Thaw blocks new Frostbite for four seconds, so nothing can be frozen in a loop. Strays, polar bears, snow golems, withers, and the FrostBear itself are immune. Heart of Winter protects against every source of Frostbite except the FrostBear's own attacks.

**Warmth** melts Frostbite: standing within three blocks of a lit campfire, fire, lava, or a magma block (the `frostbear:warmth_sources` block tag), or burning yourself, removes a stack every 1.5 seconds.

## Rewards

### Frozen Heart

Right-click for instant Heart of Winter activation—there is no eating animation. The effect grants freeze and Frostbite immunity, adds a Frostbite stack to nearby hostile mobs every two seconds, and provides a short regeneration burst.

### Glacial Core

A FrostBear crafting material used to create and repair Frostfang.

### Frostfang

A diamond-tier sword. Every hit adds Frostbite, and hits on an already-Frostbitten target deal 20% more damage. Right-click to erupt a forward wave of ice spikes, followed by a six-second cooldown.

Crafting recipe:

```text
Glacial Core
Blue Ice
Stick
```

## Requirements

| Component | Fabric | NeoForge |
| --- | --- | --- |
| Minecraft Java Edition | 26.2 | 26.2 |
| Loader | Fabric Loader 0.19.3 or newer | NeoForge 26.2.0.90 or newer |
| Fabric API | Required | — |
| Java | 25 | 25 |

The mod runs on both integrated and dedicated servers. Clients connecting to a modded server also need FrostBear (and Fabric API on Fabric) installed.

## Installation

1. Install Fabric Loader (plus Fabric API) or NeoForge for Minecraft 26.2.
2. Download the matching JAR from the GitHub Releases page, Modrinth, or CurseForge:
   - Fabric: `frostbear-fabric-26.2-<version>.jar`
   - NeoForge: `frostbear-neoforge-26.2-<version>.jar`
3. Place the JAR in the profile's `mods` directory.
4. Start Minecraft with the corresponding profile.

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

## Project layout

One Gradle build produces both loader JARs:

| Module | Contents |
| --- | --- |
| `common` | All gameplay, models, rendering, mixins, and assets. Compiled against vanilla Minecraft only. |
| `fabric` | Fabric entrypoints, registration, events, and `fabric.mod.json`. |
| `neoforge` | NeoForge entrypoint, registration, events, `neoforge.mods.toml`, and the biome modifier. |

Common code registers objects through `FrostBearPlatform`; each loader module supplies its own implementation. Put new content in `common` and only touch the loader modules for loader-specific hooks.

## Building from source

Install JDK 25, clone the repository, and run:

```powershell
.\gradlew.bat build
```

On macOS or Linux:

```bash
./gradlew build
```

The distributable JARs are generated at:

```text
fabric/build/libs/frostbear-fabric-26.2-<version>.jar
neoforge/build/libs/frostbear-neoforge-26.2-<version>.jar
```

Launch an isolated development client or server with:

```powershell
.\gradlew.bat :fabric:runClient
.\gradlew.bat :neoforge:runClient
.\gradlew.bat :fabric:runServer
.\gradlew.bat :neoforge:runServer
```

## Game tests

Gameplay tests live in `common/src/main/java/com/example/frostbear/gametest/FrostBearGameTests.java` and run on a headless server for each loader:

```powershell
.\gradlew.bat :fabric:runGametest :neoforge:runGameTestServer
```

Each loader registers the tests only in development environments (Fabric's `fabric-gametest` entrypoint, NeoForge's `RegisterGameTestsEvent`), so they never appear in players' games. To add a test, write a static method in `FrostBearGameTests`, then register it in `FrostBearFabricGameTests` and `FrostBearNeoForgeGameTests`.

## The FrostBear model

`tools/model/frostbear_model.py` is the single source of truth for the bear's geometry and texture. Edit its spec (parts, boxes, crystals, paint rules), then run:

```powershell
python tools/model/frostbear_model.py --preview build/model-preview.html
```

It regenerates `FrostBearModelLayer.java`, `frost_bear.png`, and the emissive `frost_bear_eyes.png`, and optionally writes a three.js preview that uses Minecraft's exact box-UV mapping. Serve the preview over HTTP (for example `python -m http.server`) and open it in a browser. Query options: `?view=0..3` for one large view, `?zoom=1.5`, and `?cam=x,y,z&look=x,y,z`. Pass `--pose '{"jaw": [0.8, 0, 0]}'` to preview a pose. Animations live in `FrostBearEntityModel.setupAnim`.

Do not edit the generated Java or textures by hand; changes are overwritten on the next run. The older `model-portal` and `preview` tools describe the previous model.

## License

FrostBear is available under the [MIT License](LICENSE).
