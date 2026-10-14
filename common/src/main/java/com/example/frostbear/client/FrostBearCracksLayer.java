package com.example.frostbear.client;

import com.example.frostbear.FrostBear;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/** Phase 2: fractures spread across the bear's ice and fur (generated with the model texture). */
public class FrostBearCracksLayer extends RenderLayer<FrostBearRenderState, FrostBearEntityModel> {
	private static final Identifier CRACKS = FrostBear.id("textures/entity/frost_bear_cracks.png");

	public FrostBearCracksLayer(RenderLayerParent<FrostBearRenderState, FrostBearEntityModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			FrostBearRenderState state, float yRot, float xRot) {
		if (state.enraged) {
			coloredCutoutModelCopyLayerRender(this.getParentModel(), CRACKS, poseStack, submitNodeCollector,
					lightCoords, state, -1, 1);
		}
	}
}
