package com.example.frostbear.fabric;

import com.example.frostbear.client.FrostBearEntityModel;
import com.example.frostbear.client.FrostBearEntityRenderer;
import com.example.frostbear.client.IceSpikeEntityRenderer;
import com.example.frostbear.client.ModModelLayers;
import com.example.frostbear.client.particle.FrostBreathParticle;
import com.example.frostbear.registry.ModEntities;
import com.example.frostbear.registry.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public class FrostBearFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(
				ModModelLayers.FROST_BEAR, FrostBearEntityModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.FROST_BEAR.get(), FrostBearEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.ICE_SPIKE.get(), IceSpikeEntityRenderer::new);
		ParticleProviderRegistry.getInstance().register(
				ModParticles.FROST_BREATH.get(), FrostBreathParticle.Factory::new);
	}
}
