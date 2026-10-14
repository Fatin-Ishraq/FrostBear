package com.example.frostbear.neoforge;

import com.example.frostbear.FrostBear;
import com.example.frostbear.client.FrostBearEntityModel;
import com.example.frostbear.client.FrostBearEntityRenderer;
import com.example.frostbear.client.IceSpikeEntityRenderer;
import com.example.frostbear.client.ModModelLayers;
import com.example.frostbear.client.particle.FrostBreathParticle;
import com.example.frostbear.registry.ModEntities;
import com.example.frostbear.registry.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = FrostBear.MOD_ID, value = Dist.CLIENT)
public final class FrostBearNeoForgeClient {
    private FrostBearNeoForgeClient() {}

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.FROST_BEAR, FrostBearEntityModel::getTexturedModelData);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FROST_BEAR.get(), FrostBearEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ICE_SPIKE.get(), IceSpikeEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.FROST_BREATH.get(), FrostBreathParticle.Factory::new);
    }
}
