package com.example.frostbear.client;

import com.example.frostbear.FrostBear;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.FrostBearMove;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class FrostBearEntityRenderer
		extends MobRenderer<FrostBearEntity, FrostBearRenderState, FrostBearEntityModel> {

	private static final Identifier TEXTURE =
			Identifier.fromNamespaceAndPath(FrostBear.MOD_ID, "textures/entity/frost_bear.png");

	public FrostBearEntityRenderer(EntityRendererProvider.Context context) {
		super(context, new FrostBearEntityModel(context.bakeLayer(ModModelLayers.FROST_BEAR)), 1.75F);
		this.addLayer(new FrostBearCracksLayer(this));
		this.addLayer(new FrostBearEyesFeatureRenderer(this));
	}

	@Override
	public Identifier getTextureLocation(FrostBearRenderState state) {
		return TEXTURE;
	}

	@Override
	public FrostBearRenderState createRenderState() {
		return new FrostBearRenderState();
	}

	@Override
	public void extractRenderState(FrostBearEntity entity, FrostBearRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.attackTime = entity.getAttackAnim(partialTick);
		state.hurtTime = Math.max(0.0F, entity.hurtTime - partialTick);
		state.move = entity.getMove();
		state.moveTick = state.move == FrostBearMove.NONE ? 0.0F : entity.getMoveTick() + partialTick;
		state.moveLength = entity.getMoveLength();
		state.enraged = entity.isEnraged();
		state.guardThawed = entity.isGuardThawed();
		state.aggressive = entity.isAggressive();
		state.animationOffset = (entity.getId() * 0.61803398F % 1.0F) * 1000.0F;
	}
}
