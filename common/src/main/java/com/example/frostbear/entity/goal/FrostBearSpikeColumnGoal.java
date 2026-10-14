package com.example.frostbear.entity.goal;

import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.FrostBearMove;
import com.example.frostbear.entity.IceSpikeEntity;
import com.example.frostbear.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * The answer to a target the bear cannot reach (a pillar, a ledge, the far side of a
 * wall): a stomp that erupts ice from the block the target stands on, after a frost
 * warning, with two more around it. The stomp also shatters soft blocks around the bear,
 * which digs it out of pits. In phase 2 it leaps instead when it can.
 */
public class FrostBearSpikeColumnGoal extends FrostBearAttackGoal {
	private static final int WARNING_TICKS = 12;

	private final FrostBearLeapGoal leap;

	public FrostBearSpikeColumnGoal(FrostBearEntity bear, FrostBearLeapGoal leap) {
		super(bear, FrostBearMove.SLAM, 0, 100, 40);
		this.leap = leap;
	}

	@Override
	protected boolean canStart(LivingEntity target) {
		return this.bear.isTargetUnreachable() && !this.leap.canAnswer(target);
	}

	@Override
	protected void onStart(ServerLevel level) {
		this.bear.playSound(ModSounds.FROST_BEAR_ROAR.get(), 2.0F, 0.85F);
	}

	@Override
	protected void onTick(ServerLevel level, FrostBearMove move, int tick) {
		if (tick != FrostBearMove.SLAM_IMPACT || this.target == null) {
			return;
		}
		double x = this.target.getX();
		double y = this.target.getY();
		double z = this.target.getZ();
		IceSpikeEntity.spawnSpike(level, x, y, z, 3, WARNING_TICKS, this.bear);
		for (int i = 0; i < 2; i++) {
			double angle = this.bear.getRandom().nextDouble() * Math.PI * 2.0;
			double ox = x + Math.cos(angle) * 1.6;
			double oz = z + Math.sin(angle) * 1.6;
			IceSpikeEntity.spawnSpike(level, ox, IceSpikeEntity.findGround(level, ox, y, oz), oz, 2, WARNING_TICKS + 6, this.bear);
		}
		this.bear.smashAround(level, 2);
		this.bear.clearUnreachable();
		level.sendParticles(ParticleTypes.POOF, this.bear.getX(), this.bear.getY() + 0.2, this.bear.getZ(), 18, 1.1, 0.12, 1.1, 0.04);
		this.bear.playSound(ModSounds.FROST_BEAR_STEP.get(), 2.0F, 0.6F);
	}
}
