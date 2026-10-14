package com.example.frostbear.registry;

import com.example.frostbear.FrostBear;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.IceSpikeEntity;
import com.example.frostbear.platform.RegEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity types. Each loader module registers attributes and spawn placement
 * from {@link FrostBearEntity#createFrostBearAttributes()} and
 * {@link FrostBearEntity#canNaturallySpawn}.
 */
public final class ModEntities {
	private ModEntities() {}

	public static final RegEntry<EntityType<?>, EntityType<FrostBearEntity>> FROST_BEAR = register("frost_bear",
			EntityType.Builder.of(FrostBearEntity::new, MobCategory.MONSTER)
					.sized(2.2F, 3.0F)
					.clientTrackingRange(12));

	public static final RegEntry<EntityType<?>, EntityType<IceSpikeEntity>> ICE_SPIKE = register("ice_spike",
			EntityType.Builder.<IceSpikeEntity>of(IceSpikeEntity::new, MobCategory.MISC)
					.sized(1.0F, 2.0F)
					.clientTrackingRange(10)
					.noSave());

	private static <T extends Entity> RegEntry<EntityType<?>, EntityType<T>> register(String name,
			EntityType.Builder<T> builder) {
		return FrostBear.platform().register(BuiltInRegistries.ENTITY_TYPE, name, builder::build);
	}

	public static void init() {
		FrostBear.LOGGER.info("[FrostBear] Registering entities");
	}
}
