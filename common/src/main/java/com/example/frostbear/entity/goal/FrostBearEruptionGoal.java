package com.example.frostbear.entity.goal;

import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.FrostBearMove;
import com.example.frostbear.entity.IceSpikeEntity;
import com.example.frostbear.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Ground slam: the bear rears up (the tell), slams down, and a line of ice spikes ripples
 * toward the target. Each spike marks its spot with frost before it erupts, so stepping
 * off the line dodges it. With mob griefing on, the impact shatters soft blocks around
 * the bear, so walls and pits do not hold it.
 */
public class FrostBearEruptionGoal extends FrostBearAttackGoal {
	public FrostBearEruptionGoal(FrostBearEntity bear) {
		super(bear, FrostBearMove.SLAM, 120, 220, 160);
	}

	@Override
	protected boolean canStart(LivingEntity target) {
		double d = this.distanceTo(target);
		return this.bear.onGround() && d > 3.0 && d < 18.0;
	}

	@Override
	protected void onStart(ServerLevel level) {
		this.bear.playSound(ModSounds.FROST_BEAR_ROAR.get(), 2.2F, 0.7F);
	}

	@Override
	protected boolean lookAtTarget(FrostBearMove move, int tick) {
		return tick < FrostBearMove.SLAM_IMPACT;
	}

	@Override
	protected void onTick(ServerLevel level, FrostBearMove move, int tick) {
		if (tick != FrostBearMove.SLAM_IMPACT || this.target == null) {
			return;
		}
		Vec3 dir = this.target.position().subtract(this.bear.position());
		IceSpikeEntity.spawnWave(level, this.bear.getX(), this.bear.getY(), this.bear.getZ(), dir, 6, this.bear);
		this.bear.smashAround(level, 2);
		level.sendParticles(ParticleTypes.SNOWFLAKE, this.bear.getX(), this.bear.getY() + 0.35, this.bear.getZ(),
				42, 1.5, 0.25, 1.5, 0.12);
		level.sendParticles(ParticleTypes.POOF, this.bear.getX(), this.bear.getY() + 0.2, this.bear.getZ(),
				18, 1.1, 0.12, 1.1, 0.04);
		this.bear.playSound(ModSounds.FROST_BEAR_STEP.get(), 2.0F, 0.7F);
	}
}
