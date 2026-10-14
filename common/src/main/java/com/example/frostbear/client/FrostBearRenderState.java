package com.example.frostbear.client;

import com.example.frostbear.entity.FrostBearMove;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Immutable-per-frame data consumed by the FrostBear model and render layers. */
public class FrostBearRenderState extends LivingEntityRenderState {
	public float attackTime;
	public float hurtTime;
	/** The move in progress and how far into it the bear is, in (partial) ticks. */
	public FrostBearMove move = FrostBearMove.NONE;
	public float moveTick;
	public int moveLength;
	/** Phase 2: cracked, glowing, faster. */
	public boolean enraged;
	/** Fire has melted the frontal guard for a few seconds. */
	public boolean guardThawed;
	/** True while the bear is hunting a target (vanilla's synced aggressive flag). */
	public boolean aggressive;
	/** Per-entity offset so neighbouring bears don't blink and sniff in unison. */
	public float animationOffset;

	public boolean isBusy() {
		return this.move != FrostBearMove.NONE;
	}

	public boolean is(FrostBearMove move) {
		return this.move == move;
	}
}
