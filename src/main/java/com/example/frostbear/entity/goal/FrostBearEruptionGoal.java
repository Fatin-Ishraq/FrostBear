package com.example.frostbear.entity.goal;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.IceSpikeEntity;
import com.example.frostbear.registry.ModSounds;

/**
 * The FrostBear rears up and slams the ground, erupting a wave of ice spikes
 * toward its target — the same move the Frostfang wields.
 */
public class FrostBearEruptionGoal extends Goal {
	private final FrostBearEntity bear;
	private int cooldown = 120;
	private LivingEntity target;
	private boolean fired;

	public FrostBearEruptionGoal(FrostBearEntity bear) {
		this.bear = bear;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}
		LivingEntity t = this.bear.getTarget();
		if (t == null || !t.isAlive() || this.bear.isBusy() || !this.bear.onGround()) {
			return false;
		}
		double d2 = this.bear.distanceToSqr(t);
		return d2 > 4.0 && d2 < 324.0; // roughly 2–18 blocks
	}

	@Override
	public void start() {
		this.target = this.bear.getTarget();
		this.fired = false;
		this.bear.triggerSlam();
	}

	@Override
	public boolean canContinueToUse() {
		return this.bear.getSlamTicks() > 0;
	}

	@Override
	public void tick() {
		if (this.target != null) {
			this.bear.getLookControl().setLookAt(this.target, 30.0f, 30.0f);
		}
		int elapsed = FrostBearEntity.SLAM_DURATION - this.bear.getSlamTicks();
		if (!this.fired && elapsed >= 13 && this.target != null) {
			this.fired = true;
			Vec3 dir = this.target.position().subtract(this.bear.position());
			IceSpikeEntity.spawnWave(this.bear.level(),
					this.bear.getX(), this.bear.getY(), this.bear.getZ(), dir, 6, this.bear);
			if (this.bear.level() instanceof ServerLevel world) {
				world.sendParticles(ParticleTypes.SNOWFLAKE,
						this.bear.getX(), this.bear.getY() + 0.35, this.bear.getZ(),
						42, 1.5, 0.25, 1.5, 0.12);
				world.sendParticles(ParticleTypes.POOF,
						this.bear.getX(), this.bear.getY() + 0.2, this.bear.getZ(),
						18, 1.1, 0.12, 1.1, 0.04);
			}
			this.bear.playSound(ModSounds.FROST_BEAR_STEP, 2.0f, 0.7f);
		}
	}

	@Override
	public void stop() {
		this.cooldown = 220 + this.bear.getRandom().nextInt(160);
		this.target = null;
	}
}
