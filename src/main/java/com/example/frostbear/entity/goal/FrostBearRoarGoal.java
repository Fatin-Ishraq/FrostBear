package com.example.frostbear.entity.goal;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import com.example.frostbear.entity.FrostBearEntity;

/**
 * Periodically the FrostBear rears up and lets out a bellowing roar that
 * chills and slows every player caught in the surrounding blast of frost.
 */
public class FrostBearRoarGoal extends net.minecraft.world.entity.ai.goal.Goal {
	private final FrostBearEntity bear;
	private int cooldown = 120;
	private boolean blastReleased;

	public FrostBearRoarGoal(FrostBearEntity bear) {
		this.bear = bear;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}
		return this.bear.getTarget() != null && this.bear.getTarget().isAlive()
				&& this.bear.onGround() && !this.bear.isBusy();
	}

	@Override
	public boolean canContinueToUse() {
		return this.bear.getRoarTicks() > 0;
	}

	@Override
	public void start() {
		this.blastReleased = false;
		this.bear.triggerRoar();
	}

	@Override
	public void tick() {
		if (this.bear.getTarget() != null) {
			this.bear.getLookControl().setLookAt(this.bear.getTarget(), 25.0f, 25.0f);
		}
		int elapsed = FrostBearEntity.ROAR_DURATION - this.bear.getRoarTicks();
		if (this.blastReleased || elapsed < 20) {
			return;
		}
		this.blastReleased = true;
		if (this.bear.level() instanceof ServerLevel world) {
			for (int i = 0; i < 40; i++) {
				double angle = Math.PI * 2.0 * i / 40.0;
				double radius = 2.0 + (i % 3) * 2.8;
				world.sendParticles(ParticleTypes.SNOWFLAKE,
						this.bear.getX() + Math.cos(angle) * radius,
						this.bear.getY(0.55),
						this.bear.getZ() + Math.sin(angle) * radius,
						2, 0.08, 0.18, 0.08, 0.04);
			}
			world.sendParticles(ParticleTypes.POOF,
					this.bear.getX(), this.bear.getY(0.5), this.bear.getZ(),
					24, 1.7, 1.0, 1.7, 0.09);
		}
		List<Player> nearby = this.bear.level().getEntitiesOfClass(
				Player.class, this.bear.getBoundingBox().inflate(9.0), p -> !p.isCreative() && !p.isSpectator());
		for (Player p : nearby) {
			if (p.distanceToSqr(this.bear) > 81.0 || !this.bear.hasLineOfSight(p)) {
				continue;
			}
			p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
			p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 80, 1));
			p.setTicksFrozen(p.getTicksFrozen() + 140);
			p.knockback(0.6, this.bear.getX() - p.getX(), this.bear.getZ() - p.getZ(),
					this.bear.damageSources().mobAttack(this.bear), 0.0F);
		}
	}

	@Override
	public void stop() {
		this.cooldown = 200 + this.bear.getRandom().nextInt(120);
	}
}
