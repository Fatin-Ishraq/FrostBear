package com.example.frostbear.entity.goal;

import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.FrostBearMove;
import com.example.frostbear.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 2 Frost Leap, also its answer to a target out of reach. While it crouches, a
 * frost ring follows the target; when it jumps, the ring freezes in place. Get out of the
 * ring before it lands: the landing is a shockwave that also shatters soft blocks, so it
 * reaches players on pillars, ledges, and towers.
 */
public class FrostBearLeapGoal extends FrostBearAttackGoal {
	private static final int FLIGHT_TICKS = 22;
	private static final double MIN_RANGE = 9.0;
	private static final double MAX_RANGE = 24.0;
	private static final double LAND_RADIUS = 3.5;
	private static final float LAND_DAMAGE = 10.0F;

	private Vec3 start = Vec3.ZERO;
	private Vec3 landing = Vec3.ZERO;
	private int flight;
	private int landedAt = -1;

	public FrostBearLeapGoal(FrostBearEntity bear) {
		super(bear, FrostBearMove.LEAP_CROUCH, 0, 160, 60);
	}

	@Override
	protected boolean canStart(LivingEntity target) {
		double d = this.distanceTo(target);
		return this.bear.isEnraged() && this.bear.onGround() && d <= MAX_RANGE
				&& (d >= MIN_RANGE || this.bear.isTargetUnreachable());
	}

	/** Whether a leap could answer this target right now (used by the spike column). */
	public boolean canAnswer(LivingEntity target) {
		return this.isReady() && this.bear.isEnraged() && this.distanceTo(target) <= MAX_RANGE;
	}

	@Override
	protected boolean ownsMove(FrostBearMove current) {
		return current == FrostBearMove.LEAP_CROUCH || current == FrostBearMove.LEAP;
	}

	@Override
	protected boolean lookAtTarget(FrostBearMove move, int tick) {
		return move == FrostBearMove.LEAP_CROUCH;
	}

	@Override
	protected void onStart(ServerLevel level) {
		this.landedAt = -1;
		this.bear.playSound(SoundEvents.POLAR_BEAR_WARNING, 2.2F, 0.5F);
	}

	@Override
	protected void onTick(ServerLevel level, FrostBearMove move, int tick) {
		if (move == FrostBearMove.LEAP_CROUCH) {
			if (this.target == null) {
				return;
			}
			if (tick % 2 == 0) {
				this.markLanding(level, this.target.position());
			}
			if (tick == FrostBearMove.LEAP_CROUCH.duration - 1) {
				this.start = this.bear.position();
				this.landing = this.target.position();
				this.flight = 0;
				float yaw = (float) (Mth.atan2(this.landing.z - this.start.z, this.landing.x - this.start.x) * (180.0 / Math.PI)) - 90.0F;
				this.bear.setYRot(yaw);
				this.bear.yBodyRot = yaw;
				this.bear.startMove(FrostBearMove.LEAP);
				this.bear.playSound(ModSounds.FROST_BEAR_CHARGE.get(), 2.4F, 0.6F);
			}
			return;
		}

		if (this.landedAt >= 0) {
			if (tick - this.landedAt >= FrostBearMove.LEAP_LAND_RECOVERY) {
				this.bear.endMove();
			}
			return;
		}
		this.flight++;
		if (this.flight % 2 == 0) {
			this.markLanding(level, this.landing);
		}
		double p = Math.min(1.0, this.flight / (double) FLIGHT_TICKS);
		double height = 4.0 + Math.max(0.0, this.landing.y - this.start.y);
		Vec3 desired = this.start.lerp(this.landing, p).add(0.0, height * 4.0 * p * (1.0 - p), 0.0);
		this.bear.setDeltaMovement(desired.subtract(this.bear.position()));
		this.bear.hurtMarked = true;

		boolean arrived = this.flight >= FLIGHT_TICKS && (this.bear.onGround() || this.flight >= FLIGHT_TICKS + 8);
		boolean earlyTouchdown = this.flight > FLIGHT_TICKS / 2 && this.bear.onGround();
		boolean outOfTime = tick >= FrostBearMove.LEAP.duration - FrostBearMove.LEAP_LAND_RECOVERY - 1;
		if (arrived || earlyTouchdown || outOfTime) {
			this.land(level, tick);
		}
	}

	private void land(ServerLevel level, int tick) {
		this.landedAt = tick;
		Vec3 motion = this.bear.getDeltaMovement();
		this.bear.setDeltaMovement(0.0, Math.min(0.0, motion.y), 0.0);
		this.bear.shockwave(level, this.bear.position(), LAND_RADIUS, LAND_DAMAGE, 2);
		this.bear.smashAround(level, 2);
		this.bear.clearUnreachable();
	}

	/** A ring of frost on the ground where the bear will come down. */
	private void markLanding(ServerLevel level, Vec3 at) {
		for (int i = 0; i < 16; i++) {
			double angle = Math.PI * 2.0 * i / 16.0;
			level.sendParticles(ParticleTypes.SNOWFLAKE, at.x + Math.cos(angle) * LAND_RADIUS, at.y + 0.15,
					at.z + Math.sin(angle) * LAND_RADIUS, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}
}
