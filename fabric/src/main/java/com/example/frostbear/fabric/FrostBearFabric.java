package com.example.frostbear.fabric;

import com.example.frostbear.FrostBear;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.registry.IglooSpawns;
import com.example.frostbear.registry.ModEntities;
import com.example.frostbear.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/** Fabric entrypoint: registers everything through common code, then hooks Fabric API events. */
public class FrostBearFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		FrostBear.init(new FabricPlatform());

		FabricDefaultAttributeRegistry.register(ModEntities.FROST_BEAR.get(), FrostBearEntity.createFrostBearAttributes());
		SpawnPlacements.register(ModEntities.FROST_BEAR.get(), SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, FrostBearEntity::canNaturallySpawn);
		FabricSpawns.register();

		for (var tab : ModItems.CREATIVE_TABS) {
			CreativeModeTabEvents.modifyOutputEvent(tab)
					.register(entries -> ModItems.addToCreativeTab(tab, entries::accept));
		}

		ServerChunkEvents.CHUNK_LOAD.register((world, chunk, generating) -> IglooSpawns.onChunkLoad(world, chunk));
		ServerTickEvents.END_LEVEL_TICK.register(IglooSpawns::onLevelTick);

		FrostBear.LOGGER.info("[FrostBear] Fabric initialization complete.");
	}
}
