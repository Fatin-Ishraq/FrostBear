package com.example.frostbear.entity.goal;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import com.example.frostbear.entity.FrostBearEntity;

/**
 * A periodic lunging charge: the bear roars, leaps toward its target, and on
 * impact deals bonus damage, heavy knockback, and a strong chill.
 */
public class FrostBearChargeGoal extends Goal {
	private final FrostBearEntity bear;
	private LivingEntity target;
	private int cooldown = 80;
	private int chargeTime;

	public FrostBearChargeGoal(FrostBearEntity bear) {
		this.bear = bear;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
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
		double distSq = this.bear.distanceToSqr(t);
		// Charge only from a medium distance, and only while grounded.
		return distSq > 9.0 && distSq < 256.0 && this.bear.onGround();
	}

	@Override
	public void start() {
		this.target = this.bear.getTarget();
		this.chargeTime = FrostBearEntity.CHARGE_DURATION;
		this.bear.triggerCharge();
		if (this.target != null) {
			Vec3 dir = new Vec3(
					this.target.getX() - this.bear.getX(),
					0.0,
					this.target.getZ() - this.bear.getZ()
			).normalize();
			this.bear.setDeltaMovement(dir.x * 1.7, 0.45, dir.z * 1.7);
			this.bear.hurtMarked = true;
		}
	}

	@Override
	public boolean canContinueToUse() {
		return this.chargeTime > 0 && this.target != null && this.target.isAlive();
	}

	@Override
	public void tick() {
		this.chargeTime--;
		if (this.target == null) {
			return;
		}
		this.bear.getLookControl().setLookAt(this.target, 30.0f, 30.0f);
		if (this.bear.level() instanceof ServerLevel world && this.chargeTime % 2 == 0) {
			world.sendParticles(ParticleTypes.SNOWFLAKE,
					this.bear.getX(), this.bear.getY() + 0.25, this.bear.getZ(),
					5, 0.8, 0.12, 0.8, 0.03);
		}

		if (this.bear.distanceToSqr(this.target) < 6.25) {
			this.target.knockback(2.0,
					this.bear.getX() - this.target.getX(),
					this.bear.getZ() - this.target.getZ(),
					this.bear.damageSources().mobAttack(this.bear), 0.0F);
			this.target.hurt(this.bear.damageSources().mobAttack(this.bear), 10.0f);
			this.target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 3));
			this.target.setTicksFrozen(this.target.getTicksFrozen() + 200);
			this.chargeTime = 0; // impact ends the charge
		}
	}

	@Override
	public void stop() {
		this.cooldown = 200; // ~10 seconds between charges
		this.bear.setChargeTicks(0);
		this.target = null;
	}
}
