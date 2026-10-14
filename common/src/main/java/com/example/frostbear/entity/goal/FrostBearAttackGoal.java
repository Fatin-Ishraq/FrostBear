package com.example.frostbear.entity.goal;

import java.util.EnumSet;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.FrostBearMove;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * Shared lifecycle for the FrostBear's special attacks: start a {@link FrostBearMove}
 * when the bear is free and the situation fits, run its key frames every tick, then go
 * on a cooldown that shrinks in phase 2.
 *
 * <p>Cooldowns are tracked against the bear's tick count rather than decremented in
 * {@link #canUse()}, because vanilla only evaluates idle goals every other tick.
 */
public abstract class FrostBearAttackGoal extends Goal {
	protected final FrostBearEntity bear;
	private final FrostBearMove move;
	private final int baseCooldown;
	private final int cooldownJitter;
	private int readyAt;
	protected @Nullable LivingEntity target;

	protected FrostBearAttackGoal(FrostBearEntity bear, FrostBearMove move, int firstDelay, int baseCooldown, int cooldownJitter) {
		this.bear = bear;
		this.move = move;
		this.baseCooldown = baseCooldown;
		this.cooldownJitter = cooldownJitter;
		this.readyAt = firstDelay;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	/** Whether this attack fits right now, given a live target and a free bear. */
	protected abstract boolean canStart(LivingEntity target);

	/** Runs every tick while the move is active; {@code tick} counts from 0. */
	protected abstract void onTick(ServerLevel level, FrostBearMove move, int tick);

	protected void onStart(ServerLevel level) {
	}

	protected void onStop() {
	}

	/** Moves that belong to this attack (multi-stage attacks chain several). */
	protected boolean ownsMove(FrostBearMove current) {
		return current == this.move;
	}

	protected boolean lookAtTarget(FrostBearMove move, int tick) {
		return true;
	}

	public boolean isReady() {
		return this.bear.tickCount >= this.readyAt;
	}

	protected void delay(int ticks) {
		this.readyAt = Math.max(this.readyAt, this.bear.tickCount + ticks);
	}

	@Override
	public boolean canUse() {
		LivingEntity t = this.bear.getTarget();
		return this.isReady() && t != null && t.isAlive() && this.bear.canStartMove() && this.canStart(t);
	}

	@Override
	public void start() {
		this.target = this.bear.getTarget();
		this.bear.startMove(this.move);
		if (this.bear.level() instanceof ServerLevel level) {
			this.onStart(level);
		}
	}

	@Override
	public boolean canContinueToUse() {
		return this.ownsMove(this.bear.getMove());
	}

	@Override
	public void tick() {
		FrostBearMove current = this.bear.getMove();
		int tick = this.bear.getMoveTick();
		if (this.target != null && this.lookAtTarget(current, tick)) {
			this.bear.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
		}
		if (this.bear.level() instanceof ServerLevel level) {
			this.onTick(level, current, tick);
		}
	}

	@Override
	public void stop() {
		int jitter = this.cooldownJitter > 0 ? this.bear.getRandom().nextInt(this.cooldownJitter) : 0;
		this.readyAt = this.bear.tickCount + this.bear.cooldown(this.baseCooldown + jitter);
		this.onStop();
		this.target = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true; // key frames land on exact ticks
	}

	protected double distanceTo(LivingEntity target) {
		return Math.sqrt(this.bear.distanceToSqr(target));
	}
}
