package com.example.frostbear.registry;

import com.example.frostbear.FrostBear;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.world.entity.MobCategory;

public final class ModSpawns {
	private ModSpawns() {}

	// Group size 1,1 → only ever one per spawn attempt. Kept low on purpose: this is a
	// 260 HP boss, and at wolf-like weight (8) the snowy biomes fill with them and every
	// one of them ticks boss AI. 3 still makes them findable without dragging the server.
	private static final int WEIGHT = 3;

	public static void register() {
		FrostBear.LOGGER.info("[FrostBear] Registering natural spawns");
		BiomeModifications.addSpawn(
				BiomeSelectors.tag(ConventionalBiomeTags.IS_SNOWY)
						.or(BiomeSelectors.tag(ConventionalBiomeTags.IS_ICY)),
				MobCategory.MONSTER, ModEntities.FROST_BEAR, WEIGHT, 1, 1);
	}
}
