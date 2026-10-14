# Changelog

## 2.0.0 (in development)

### The boss fight, reimagined

- Two phases. Below half health the bear roars into **Heart of Winter**: it cracks, its crystals blaze, it moves faster, and it adds the **Ice Ring**, **Frost Leap**, and **Glacial Cataclysm**.
- Every attack now has a readable tell of at least ¾ of a second and a counter:
  - The bite became a **Maul** (swipe, swipe, bite) that breaks shields.
  - The **Charge** winds up, then locks its direction; crashing into a solid wall stuns the bear for 2.5 seconds.
  - **Frost Breath** inhales first, then sweeps once instead of tracking.
  - Slam spikes mark their spot with frost before erupting.
- **Glacial Cataclysm**: a three-second channel; hide behind blocks or interrupt it with 30 damage or fire to stun the bear.
- **Frontal guard**: 40% less damage from the front. **Fire** melts the guard for five seconds. **Stunned**, it takes double damage.
- **No safe spots**: a target it cannot reach gets a spike column from the block it stands on (or a leap in phase 2), and with mob griefing on, slams and landings shatter soft blocks.
- **Leash**: with no player nearby for 15 seconds, it heals and resets. Health scales with nearby players.
- Slowness and Weakness last half as long on the bear; its own frost pierces Heart of Winter.
- **Warmth** (campfires, fire, lava, magma blocks, burning) melts Frostbite.
- 500 health, 10 armour, 4 armour toughness, and fall-damage immunity (for its leaps).
- Ice spikes no longer seal mobs or players inside their blocks.
- Fixed the bear standing still when it could not path to a nearby target, and an AI cooldown bug that made every special attack wait twice as long as intended.
- The face animates: it blinks, scowls when hostile, sniffs and yawns at rest, squints into its breath, and is dazed when stunned.
- Game tests cover the guard, fire, stun, phase change, leash, warmth, Heart of Winter piercing, resistances, block smashing, spikes, and two AI scenarios (mauling a target in reach, answering a target on a pillar).

### Frostbite

- Added **Frostbite**, a stacking cold effect (up to 5 stacks). Each stack slows by 8%; at 3 or more the victim cannot sprint.
- Reaching 5 stacks **Shatters** the target for 7 damage, credited to the attacker, and grants **Thaw**, which blocks new Frostbite for 4 seconds.
- FrostBear bites, roars, frost breath, charges, and ice spikes now apply Frostbite instead of raw Slowness and freezing.
- Frostfang applies Frostbite on hit and deals +20% damage to Frostbitten targets.
- Heart of Winter grants Frostbite immunity and adds a stack to nearby hostile mobs every 2 seconds.
- Added game tests for Frostbite, Thaw, immunity, and the Frostfang bonus; they run on both loaders.

## Unreleased — Fabric and NeoForge 26.2

- One build now produces both the Fabric and NeoForge JARs from a shared `common` module.
- Release JARs are named `frostbear-fabric-26.2-<version>.jar` and `frostbear-neoforge-26.2-<version>.jar`.
- NeoForge natural spawns now cover icy biomes (`#c:is_icy`) as well as snowy ones, matching Fabric.
- The FrostBear boss bar now shows the bear's custom name after it is name-tagged or reloaded from a save.
- Built against Fabric Loader 0.19.5, Fabric API 0.161.0+26.2, and NeoForge 26.2.0.90.

## 1.1.0 — Minecraft 26.2

- Ported FrostBear to Minecraft Java Edition 26.2 and Java 25.
- Rebuilt the FrostBear model silhouette, head, jaw, eyes, neck, and connected back crystal.
- Added current entity render-state and model-layer support.
- Added natural snowy-biome spawning and guarded igloo encounters.
- Added polar-bear conversion through Slowness and Weakness.
- Added the Frozen Heart with instant Heart of Winter activation.
- Added the Glacial Core and craftable Frostfang weapon.
- Added Frostfang's ice-spike wave ability.
- Updated item definitions, recipe-book support, loot, particles, sounds, mixins, and saved data for 26.2.
