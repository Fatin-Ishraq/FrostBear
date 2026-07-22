package com.example.frostbear.client;

import com.example.frostbear.FrostBear;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Renders the FrostBear's eyes and restrained crystal fissures fullbright. */
public class FrostBearEyesFeatureRenderer
		extends EyesLayer<FrostBearRenderState, FrostBearEntityModel> {

	private static final RenderType EYES =
			RenderTypes.eyes(Identifier.fromNamespaceAndPath(FrostBear.MOD_ID, "textures/entity/frost_bear_eyes.png"));

	public FrostBearEyesFeatureRenderer(RenderLayerParent<FrostBearRenderState, FrostBearEntityModel> parent) {
		super(parent);
	}

	@Override
	public RenderType renderType() {
		return EYES;
	}
}