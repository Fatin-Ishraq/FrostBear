package com.example.frostbear.entity.goal;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.registry.ModParticles;

/**
 * The FrostBear plants itself and exhales a cone of freezing breath: custom
 * frost particles stream forward while anything caught in the cone takes chip
 * damage, Slowness and frost buildup, and a gentle pushback.
 */
public class FrostBearBreathGoal extends Goal {
	private static final double RANGE = 9.0;
	private static final double CONE_COS = Math.cos(Math.toRadians(38.0));

	private final FrostBearEntity bear;
	private int cooldown = 160;
	private LivingEntity target;

	public FrostBearBreathGoal(FrostBearEntity bear) {
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
		if (t == null || !t.isAlive() || this.bear.isBusy()) {
			return false;
		}
		double d2 = this.bear.distanceToSqr(t);
		return d2 > 16.0 && d2 < 289.0 && this.bear.hasLineOfSight(t);
	}

	@Override
	public void start() {
		this.target = this.bear.getTarget();
		this.bear.triggerBreath();
	}

	@Override
	public boolean canContinueToUse() {
		return this.bear.getBreathTicks() > 0 && this.target != null && this.target.isAlive();
	}

	@Override
	public void tick() {
		if (this.target != null) {
			this.bear.getLookControl().setLookAt(this.target, 30.0f, 30.0f);
		}
		int elapsed = FrostBearEntity.BREATH_DURATION - this.bear.getBreathTicks();
		if (elapsed >= 8) { // exhale after the wind-up
			emitCone(elapsed % 4 == 0);
		}
	}

	private void emitCone(boolean damageTick) {
		if (!(this.bear.level() instanceof ServerLevel world)) {
			return;
		}
		RandomSource random = this.bear.getRandom();
		Vec3 look = this.bear.getViewVector(1.0f).normalize();
		Vec3 mouth = this.bear.getEyePosition().add(look.scale(1.6)).add(0.0, -0.3, 0.0);

		for (int i = 0; i < 6; i++) {
			double dist = 1.0 + random.nextDouble() * RANGE;
			double spread = 0.18 * dist;
			double px = mouth.x + look.x * dist + (random.nextDouble() - 0.5) * spread;
			double py = mouth.y + look.y * dist + (random.nextDouble() - 0.5) * spread;
			double pz = mouth.z + look.z * dist + (random.nextDouble() - 0.5) * spread;
			world.sendParticles(ModParticles.FROST_BREATH, px, py, pz, 1, look.x * 0.2, look.y * 0.2, look.z * 0.2, 0.05);
			world.sendParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 1, 0.0, 0.0, 0.0, 0.01);
		}
		if (!damageTick) {
			return;
		}

		List<LivingEntity> victims = world.getEntitiesOfClass(LivingEntity.class,
				this.bear.getBoundingBox().inflate(RANGE),
				e -> e != this.bear && !(e instanceof FrostBearEntity) && e.isAlive());
		for (LivingEntity victim : victims) {
			Vec3 to = victim.getEyePosition().subtract(mouth);
			double d = to.length();
			if (d > RANGE || look.dot(to.normalize()) < CONE_COS || !this.bear.hasLineOfSight(victim)) {
				continue;
			}
			victim.hurt(this.bear.damageSources().freeze(), 2.0f);
			victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
			victim.setTicksFrozen(victim.getTicksFrozen() + 30);
			victim.push(look.x * 0.1, 0.02, look.z * 0.1);
			victim.hurtMarked = true;
		}
	}

	@Override
	public void stop() {
		this.cooldown = 220 + this.bear.getRandom().nextInt(120);
		this.target = null;
	}
}
