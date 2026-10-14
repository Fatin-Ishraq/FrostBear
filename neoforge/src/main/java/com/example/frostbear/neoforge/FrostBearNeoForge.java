package com.example.frostbear.neoforge;

import com.example.frostbear.FrostBear;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.registry.IglooSpawns;
import com.example.frostbear.registry.ModEntities;
import com.example.frostbear.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

/**
 * NeoForge entrypoint: registers everything through common code, then hooks
 * NeoForge events. Natural spawns come from the biome modifier in
 * data/frostbear/neoforge/biome_modifier.
 */
@Mod(FrostBear.MOD_ID)
public class FrostBearNeoForge {
	public FrostBearNeoForge(IEventBus modBus) {
		NeoForgePlatform platform = new NeoForgePlatform();
		FrostBear.init(platform);
		platform.registerAll(modBus);
		if (GameTestHooks.isGametestEnabled()) {
			FrostBearNeoForgeGameTests.register(modBus);
		}

		modBus.addListener(EntityAttributeCreationEvent.class, event ->
				event.put(ModEntities.FROST_BEAR.get(), FrostBearEntity.createFrostBearAttributes().build()));
		modBus.addListener(RegisterSpawnPlacementsEvent.class, event -> event.register(
				ModEntities.FROST_BEAR.get(),
				SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				FrostBearEntity::canNaturallySpawn,
				RegisterSpawnPlacementsEvent.Operation.REPLACE));
		modBus.addListener(BuildCreativeModeTabContentsEvent.class, event ->
				ModItems.addToCreativeTab(event.getTabKey(), event::accept));

		NeoForge.EVENT_BUS.addListener(ChunkEvent.Load.class, event -> {
			if (event.getLevel() instanceof ServerLevel level) {
				IglooSpawns.onChunkLoad(level, event.getChunk());
			}
		});
		NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
			if (event.getLevel() instanceof ServerLevel level) {
				IglooSpawns.onLevelTick(level);
			}
		});

		FrostBear.LOGGER.info("[FrostBear] NeoForge initialization registered.");
	}
}
