package com.example.frostbear;

import com.example.frostbear.registry.IglooSpawns;
import com.example.frostbear.registry.ModEffects;
import com.example.frostbear.registry.ModEntities;
import com.example.frostbear.registry.ModItems;
import com.example.frostbear.registry.ModParticles;
import com.example.frostbear.registry.ModSounds;
import com.example.frostbear.registry.ModSpawns;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod entrypoint (runs on both client and server / dedicated server).
 * Client-only rendering is set up in {@link com.example.frostbear.client.FrostBearClient}.
 */
public class FrostBear implements ModInitializer {
	public static final String MOD_ID = "frostbear";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[FrostBear] The frozen north stirs...");

		// Order matters: entities must be registered before the spawn egg that references them.
		ModSounds.register();
		ModEffects.register();
		ModParticles.register();
		ModEntities.register();
		ModItems.register();
		ModSpawns.register();
		IglooSpawns.register();

		LOGGER.info("[FrostBear] Initialized.");
	}
}
