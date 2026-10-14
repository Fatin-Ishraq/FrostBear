package com.example.frostbear.fabric;

import com.example.frostbear.FrostBear;
import com.example.frostbear.registry.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.world.entity.MobCategory;

/** Natural spawns. The NeoForge equivalent is data/frostbear/neoforge/biome_modifier/frost_bear_spawns.json. */
final class FabricSpawns {
	private FabricSpawns() {}

	// Group size 1,1 → only ever one per spawn attempt. Kept low on purpose: this is a
	// 260 HP boss, and at wolf-like weight (8) the snowy biomes fill with them and every
	// one of them ticks boss AI. 3 still makes them findable without dragging the server.
	private static final int WEIGHT = 3;

	static void register() {
		FrostBear.LOGGER.info("[FrostBear] Registering natural spawns");
		BiomeModifications.addSpawn(
				BiomeSelectors.tag(ConventionalBiomeTags.IS_SNOWY)
						.or(BiomeSelectors.tag(ConventionalBiomeTags.IS_ICY)),
				MobCategory.MONSTER, ModEntities.FROST_BEAR.get(), WEIGHT, 1, 1);
	}
}
