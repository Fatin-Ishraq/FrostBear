package com.example.frostbear.client;

import com.example.frostbear.FrostBear;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Renders the FrostBear's eyes and crystal veins fullbright. In phase 2 it switches to a
 * brighter texture where the crystals and the cracks in its ice glow too.
 */
public class FrostBearEyesFeatureRenderer extends RenderLayer<FrostBearRenderState, FrostBearEntityModel> {
	private static final RenderType CALM = RenderTypes.eyes(FrostBear.id("textures/entity/frost_bear_eyes.png"));
	private static final RenderType ENRAGED = RenderTypes.eyes(FrostBear.id("textures/entity/frost_bear_glow_enraged.png"));

	public FrostBearEyesFeatureRenderer(RenderLayerParent<FrostBearRenderState, FrostBearEntityModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			FrostBearRenderState state, float yRot, float xRot) {
		submitNodeCollector.order(2).submitModel(this.getParentModel(), state, poseStack,
				state.enraged ? ENRAGED : CALM, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
	}
}
