package com.example.frostbear.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Immutable-per-frame data consumed by the FrostBear model and render layers. */
public class FrostBearRenderState extends LivingEntityRenderState {
	public float attackTime;
	public float hurtTime;
	public float roarTicks;
	public float breathTicks;
	public float slamTicks;
	public float chargeTicks;

	public boolean isBusy() {
		return this.roarTicks > 0.0F || this.breathTicks > 0.0F
				|| this.slamTicks > 0.0F || this.chargeTicks > 0.0F;
	}
}