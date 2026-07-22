package com.example.frostbear.client;

import com.example.frostbear.client.particle.FrostBreathParticle;
import com.example.frostbear.registry.ModEntities;
import com.example.frostbear.registry.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public class FrostBearClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(
				ModModelLayers.FROST_BEAR, FrostBearEntityModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.FROST_BEAR, FrostBearEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.ICE_SPIKE, IceSpikeEntityRenderer::new);
		ParticleProviderRegistry.getInstance().register(
				ModParticles.FROST_BREATH, FrostBreathParticle.Factory::new);
	}
}