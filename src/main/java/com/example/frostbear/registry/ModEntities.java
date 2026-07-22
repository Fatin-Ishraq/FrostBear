package com.example.frostbear.registry;

import com.example.frostbear.FrostBear;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.IceSpikeEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	private ModEntities() {
	}

	public static final EntityType<FrostBearEntity> FROST_BEAR = register("frost_bear",
			EntityType.Builder.of(FrostBearEntity::new, MobCategory.MONSTER)
					.sized(2.2F, 3.0F)
					.clientTrackingRange(12));

	public static final EntityType<IceSpikeEntity> ICE_SPIKE = register("ice_spike",
			EntityType.Builder.<IceSpikeEntity>of(IceSpikeEntity::new, MobCategory.MISC)
					.sized(1.0F, 2.0F)
					.clientTrackingRange(10)
					.noSave());

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		Identifier id = Identifier.fromNamespaceAndPath(FrostBear.MOD_ID, name);
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void register() {
		FrostBear.LOGGER.info("[FrostBear] Registering entities");
		FabricDefaultAttributeRegistry.register(FROST_BEAR, FrostBearEntity.createFrostBearAttributes());
		SpawnPlacements.register(FROST_BEAR, SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, FrostBearEntity::canNaturallySpawn);
	}
}