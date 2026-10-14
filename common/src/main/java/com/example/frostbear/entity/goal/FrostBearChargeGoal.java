package com.example.frostbear.entity.goal;

import java.util.List;
import com.example.frostbear.effect.Frostbite;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.FrostBearMove;
import com.example.frostbear.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Charge. The tell is a second of scraping the ground with the head lowered; the
 * direction locks at the end of it, so a sidestep makes it miss. It rushes in a straight
 * line, trampling the first thing it meets. Crashing into a wall it cannot shatter gets
 * its head stuck in the ice: a stun that doubles all damage it takes.
 */
public class FrostBearChargeGoal extends FrostBearAttackGoal {
	private static final double SPEED = 0.9;
	private static final float HIT_DAMAGE = 12.0F;

	private Vec3 direction = Vec3.ZERO;
	private float yaw;

	public FrostBearChargeGoal(FrostBearEntity bear) {
		super(bear, FrostBearMove.CHARGE_WINDUP, 80, 200, 60);
	}

	@Override
	protected boolean canStart(LivingEntity target) {
		double d = this.distanceTo(target);
		return this.bear.onGround() && d > 6.0 && d < 18.0 && this.bear.hasLineOfSight(target);
	}

	@Override
	protected boolean ownsMove(FrostBearMove current) {
		return current == FrostBearMove.CHARGE_WINDUP || current == FrostBearMove.CHARGE;
	}

	@Override
	protected boolean lookAtTarget(FrostBearMove move, int tick) {
		return move == FrostBearMove.CHARGE_WINDUP;
	}

	@Override
	protected void onStart(ServerLevel level) {
		this.bear.playSound(SoundEvents.POLAR_BEAR_WARNING, 2.0F, 0.8F);
	}

	@Override
	protected void onTick(ServerLevel level, FrostBearMove move, int tick) {
		if (move == FrostBearMove.CHARGE_WINDUP) {
			this.windUp(level, tick);
		} else if (move == FrostBearMove.CHARGE) {
			this.rush(level, tick);
		}
	}

	private void windUp(ServerLevel level, int tick) {
		if (tick % 3 == 0) {
			BlockState ground = level.getBlockState(this.bear.blockPosition().below());
			if (!ground.isAir()) {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
						this.bear.getX(), this.bear.getY() + 0.1, this.bear.getZ(), 10, 1.0, 0.05, 1.0, 0.15);
			}
		}
		if (tick == FrostBearMove.CHARGE_WINDUP.duration - 1 && this.target != null) {
			Vec3 to = new Vec3(this.target.getX() - this.bear.getX(), 0.0, this.target.getZ() - this.bear.getZ());
			this.direction = to.lengthSqr() > 1.0e-4 ? to.normalize() : Vec3.ZERO;
			this.yaw = (float) (Mth.atan2(this.direction.z, this.direction.x) * (180.0 / Math.PI)) - 90.0F;
			this.bear.startMove(FrostBearMove.CHARGE);
			this.bear.playSound(ModSounds.FROST_BEAR_CHARGE.get(), 2.2F, 1.0F);
		}
	}

	private void rush(ServerLevel level, int tick) {
		Vec3 motion = this.bear.getDeltaMovement();
		this.bear.setDeltaMovement(this.direction.x * SPEED, motion.y, this.direction.z * SPEED);
		this.bear.setYRot(this.yaw);
		this.bear.yBodyRot = this.yaw;
		this.bear.yHeadRot = this.yaw;
		if (tick % 2 == 0) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, this.bear.getX(), this.bear.getY() + 0.25, this.bear.getZ(),
					5, 0.8, 0.12, 0.8, 0.03);
		}

		List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, this.bear.getBoundingBox().inflate(0.6),
				e -> e != this.bear && e.isAlive());
		if (!hit.isEmpty()) {
			for (LivingEntity victim : hit) {
				victim.hurtServer(level, this.bear.damageSources().mobAttack(this.bear), HIT_DAMAGE);
				victim.knockback(2.0, this.bear.getX() - victim.getX(), this.bear.getZ() - victim.getZ(),
						this.bear.damageSources().mobAttack(this.bear), HIT_DAMAGE);
				victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
				Frostbite.apply(victim, 2, this.bear);
			}
			level.playSound(null, this.bear.getX(), this.bear.getY(), this.bear.getZ(),
					SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.5F);
			this.bear.endMove();
			return;
		}

		if (tick > 2 && this.bear.horizontalCollision) {
			// crash through snow and dirt; anything harder stuns it
			Vec3 front = this.bear.position().add(this.direction.scale(1.9));
			BlockPos center = BlockPos.containing(front);
			int broken = this.bear.smashSoftBlocks(level, center.offset(-1, 0, -1), center.offset(1, 3, 1), 18);
			if (broken == 0) {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(center.above())),
						front.x, front.y + 1.5, front.z, 30, 0.6, 0.6, 0.6, 0.2);
				level.playSound(null, front.x, front.y, front.z, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 1.4F, 0.5F);
				this.bear.setDeltaMovement(Vec3.ZERO);
				this.bear.stun(level, FrostBearEntity.CHARGE_STUN_TICKS);
			}
		}
	}

	@Override
	protected void onStop() {
		Vec3 motion = this.bear.getDeltaMovement();
		this.bear.setDeltaMovement(motion.x * 0.3, motion.y, motion.z * 0.3);
	}
}
