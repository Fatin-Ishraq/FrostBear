# FrostBear — notes for Claude

FrostBear is a Minecraft Java Edition boss mod (a frozen polar bear) for Fabric and NeoForge. The owner is Fatin-Ishraq. The repository is public on GitHub.

Read `README.md` for what the mod does (the fight, Frostbite, rewards) and `CHANGELOG.md` for what v2 changed. This file covers what those don't: how we work, the state of the branches, and what to do next.

## Working agreements

- **Scope:** keep the mod medium-sized and high quality. Polish what exists rather than adding many new items, mobs, or systems. Ask the owner before adding new content.
- **Discuss first:** for design work (new mechanics, reworks), describe the idea and get approval before writing code.
- **Git:** commit only when the owner asks. End commit messages with:
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`
- **Pushing:** don't push branches other than the one you're working on without asking.
- **Tests:** run the game tests on both loaders before calling gameplay work done.

## Branches

| Branch | State |
| --- | --- |
| `v2/26.2` | **Active.** v2 for Minecraft 26.2 (multiloader, new model, Frostbite, two-phase fight). Version `2.0.0-alpha.1`. |
| `main` | Released 1.1.0 (Fabric only, 26.2). v2 has not been merged yet. |

These branches exist only on the owner's PC and are not on GitHub: `multiloader/26.1.2` (the multiloader build for 26.1.2, without v2 features), and the older per-loader fix branches `codex/mc-26.2-neoforge` and `codex/mc-26.1.2-neoforge`.

## Building and testing

You need JDK 25. Gradle is 9.5.1, through the wrapper.

```bash
./gradlew build
./gradlew :fabric:runGametest :neoforge:runGameTestServer
```

There are 17 game tests in `common/src/main/java/com/example/frostbear/gametest/FrostBearGameTests.java`, and all of them pass on both loaders. When you add a test, register it in both `fabric/.../FrostBearFabricGameTests.java` and `neoforge/.../FrostBearNeoForgeGameTests.java`.

- **Cache errors:** if Gradle reports corrupt caches, run builds one at a time (don't run two Gradle builds in parallel).
- **Playtesting:** a cloud session can't open the game window. Playtesting (`./gradlew :fabric:runClient`) happens on the owner's PC, so ask them for feel and balance feedback.

## Layout

- **`common`:** all gameplay, rendering, mixins, and assets. It compiles against vanilla (NeoForm `26.2-2`). It registers objects through `platform/FrostBearPlatform` (`register(registry, name, factory)` returns a `RegEntry`).
- **`fabric`, `neoforge`:** each holds only its entrypoints, registration, and events. Fabric registers with `Registry.registerForHolder`; NeoForge uses `DeferredRegister`.
- **`tools/model/frostbear_model.py`:** the single source of truth for the bear's geometry and textures. It generates `FrostBearModelLayer.java` and the `frost_bear*.png` textures, so never edit those by hand. Run `python tools/model/frostbear_model.py --preview build/model-preview.html` for a three.js preview. It supports `--pose` and `--enraged`.
  - Crystals are auto-seated onto the body by `seat_crystals`. The owner once reported floating crystals, so check the front view after any geometry change.

### The fight code

- **`entity/FrostBearEntity.java`:** the boss.
  - A move state machine (`startMove`, `tickMove`, `endMove`) with synced `MOVE`, `MOVE_TICK`, and `PHASE`.
  - Phases (phase 2 starts at 50% health), the frontal guard, fire thaw, stun, the Cataclysm channel, the leash, and player scaling.
  - Reach handling: spike column or leap when the target can't be reached.
  - Soft-block smashing, controlled by the `frostbear:frost_bear_breakable` tag and the `mobGriefing` game rule.
- **`entity/FrostBearMove.java`:** each move's duration and key frames. Both the server goals and the client animations use these.
- **`entity/goal/*`:** one goal per attack.
  - The attack goals extend `FrostBearAttackGoal`. Its cooldowns use `readyAt` compared against `tickCount`, because idle goals only tick every other tick.
  - The chase goal (`FrostBearChaseGoal`) extends `MeleeAttackGoal` and runs the Maul.
- **`client/model/FrostBearEntityModel.java`:** per-move body animation and the face (lids, brows, ears, sniff, yawn).
- **`effect/Frostbite.java`:** stacking, Shatter, Thaw, Heart of Winter piercing, and warmth (`frostbear:warmth_sources` tag).

## 26.2 API gotchas

The game is unobfuscated, so the code uses Mojang names.

- Entity-type constants are in `EntityTypes`.
- Tag checks use `entity.is(tag)`.
- `knockback` takes five arguments: `knockback(power, x, z, source, float)`.
- Game rules: `level.getGameRules().get(GameRules.MOB_GRIEFING)`.
- Disable a shield with `BlocksAttacks.disable(...)`.
- Render layers implement `submit(PoseStack, SubmitNodeCollector, light, state, yRot, xRot)`.
- Never change an entity's effects from inside an effect tick. That throws `ConcurrentModificationException`, so defer it with `server.schedule(server.wrapRunnable(...))`.

## To-do

1. **Playtest tuning:** the fight has passed the tests but hasn't been played much yet. Check these with the owner:
   - Overall feel and difficulty. The target is a 3–5 minute fight for one player in diamond gear.
   - The Frost Leap and its tracking ring.
   - The Glacial Cataclysm cover check (the clip test from the bear to the player).
   - Multiplayer health scaling.
   - Whether breaking soft blocks feels fair.
2. **Port v2 to 26.1.2:** start from the `multiloader/26.1.2` branch (ask the owner to push it first) and bring the v2 changes over. Known differences on 26.1.2:
   - `knockback` takes three arguments.
   - Use `gui.setOverlayMessage`.
   - Use `logoFile` in the mod metadata.
   - Versions: NeoForm `26.1.2-1`, NeoForge `26.1.2.117`, Fabric API `0.155.3+26.1.2`.
   - Regenerate the game-test structure `platform.nbt` with the 26.1.2 DataVersion.
3. **Release prep:** finish `2.0.0` (drop `-alpha`), update `MODRINTH_DESCRIPTION.md`, add new screenshots, and merge into `main`. Ask the owner first.
4. **Branch cleanup:** decide with the owner which of the PC-only branches to push or delete.

## Later ideas (not approved yet; discuss before building)

These came up during v2 planning. The owner wants the mod to stay medium-sized.

- Upgrade the rewards (Frostfang, Frozen Heart) to match the new fight.
- An arena or lair for the igloo guardian.
- More sound design for the tells.
