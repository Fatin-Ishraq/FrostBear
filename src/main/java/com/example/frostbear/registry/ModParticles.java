package com.example.frostbear.registry;

import com.example.frostbear.FrostBear;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModParticles {
	private ModParticles() {}

	public static final SimpleParticleType FROST_BREATH = Registry.register(
			BuiltInRegistries.PARTICLE_TYPE,
			Identifier.fromNamespaceAndPath(FrostBear.MOD_ID, "frost_breath"),
			FabricParticleTypes.simple());

	public static void register() {
		FrostBear.LOGGER.info("[FrostBear] Registering particles");
	}
}
