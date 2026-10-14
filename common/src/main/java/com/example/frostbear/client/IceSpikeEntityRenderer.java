package com.example.frostbear.client;

import com.example.frostbear.entity.IceSpikeEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** The transient IceSpike entity is invisible; its synchronized ice blocks are the visual. */
public class IceSpikeEntityRenderer extends EntityRenderer<IceSpikeEntity, EntityRenderState> {
	public IceSpikeEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}
}