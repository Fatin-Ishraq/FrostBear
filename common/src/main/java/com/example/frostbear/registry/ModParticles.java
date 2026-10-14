package com.example.frostbear.registry;

import com.example.frostbear.FrostBear;
import com.example.frostbear.platform.RegEntry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModParticles {
	private ModParticles() {}

	// The SimpleParticleType constructor is protected in vanilla, hence the anonymous subclass.
	public static final RegEntry<ParticleType<?>, SimpleParticleType> FROST_BREATH = FrostBear.platform().register(
			BuiltInRegistries.PARTICLE_TYPE, "frost_breath",
			key -> new SimpleParticleType(false) {});

	public static void init() {
		FrostBear.LOGGER.info("[FrostBear] Registering particles");
	}
}
