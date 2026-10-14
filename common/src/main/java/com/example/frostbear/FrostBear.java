package com.example.frostbear;

import com.example.frostbear.platform.FrostBearPlatform;
import com.example.frostbear.registry.ModEffects;
import com.example.frostbear.registry.ModEntities;
import com.example.frostbear.registry.ModItems;
import com.example.frostbear.registry.ModParticles;
import com.example.frostbear.registry.ModSounds;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader-independent entrypoint. The Fabric and NeoForge modules call
 * {@link #init} first, then wire up their own events.
 */
public final class FrostBear {
	public static final String MOD_ID = "frostbear";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static FrostBearPlatform platform;

	private FrostBear() {}

	public static void init(FrostBearPlatform loaderPlatform) {
		platform = loaderPlatform;
		LOGGER.info("[FrostBear] The frozen north stirs...");

		// Order matters on Fabric: entities must be registered before the spawn egg that references them.
		ModSounds.init();
		ModEffects.init();
		ModParticles.init();
		ModEntities.init();
		ModItems.init();
	}

	public static FrostBearPlatform platform() {
		if (platform == null) {
			throw new IllegalStateException("FrostBear.init has not run yet");
		}
		return platform;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
