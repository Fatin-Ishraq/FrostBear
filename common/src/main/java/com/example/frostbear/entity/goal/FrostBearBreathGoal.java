package com.example.frostbear.entity.goal;

import java.util.List;
import com.example.frostbear.effect.Frostbite;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.FrostBearMove;
import com.example.frostbear.registry.ModParticles;
import com.example.frostbear.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Frost breath. The tell is an audible inhale with frost drawn into the jaw; then the
 * bear plants itself and sweeps a cone of freezing breath across the target's position
 * in one pass rather than tracking. Step out of the sweep, or get behind it: the bear is
 * rooted, so its flanks are open the whole time.
 */
public class FrostBearBreathGoal extends FrostBearAttackGoal {
	private static final double RANGE = 9.0;
	private static final double CONE_COS = Math.cos(Math.toRadians(32.0));
	private static final float SWEEP_DEGREES = 28.0F;

	private float baseYaw;

	public FrostBearBreathGoal(FrostBearEntity bear) {
		super(bear, FrostBearMove.BREATH, 160, 240, 120);
	}

	@Override
	protected boolean canStart(LivingEntity target) {
		double d = this.distanceTo(target);
		return d > 4.0 && d < 15.0 && this.bear.hasLineOfSight(target);
	}

	@Override
	protected void onStart(ServerLevel level) {
		this.bear.playSound(SoundEvents.POLAR_BEAR_WARNING, 2.0F, 0.6F);
	}

	@Override
	protected boolean lookAtTarget(FrostBearMove move, int tick) {
		return tick < FrostBearMove.BREATH_EXHALE;
	}

	@Override
	protected void onTick(ServerLevel level, FrostBearMove move, int tick) {
		if (tick < FrostBearMove.BREATH_EXHALE) {
			this.inhale(level, tick);
			return;
		}
		if (tick == FrostBearMove.BREATH_EXHALE && this.target != null) {
			this.baseYaw = (float) (Mth.atan2(this.target.getZ() - this.bear.getZ(), this.target.getX() - this.bear.getX())
					* (180.0 / Math.PI)) - 90.0F;
			this.bear.playSound(ModSounds.FROST_BEAR_BREATH.get(), 2.0F, 1.0F);
		}
		if (tick >= FrostBearMove.BREATH_END) {
			return;
		}
		// one pass from one side to the other
		float progress = (tick - FrostBearMove.BREATH_EXHALE) / (float) (FrostBearMove.BREATH_END - FrostBearMove.BREATH_EXHALE);
		float yaw = (this.baseYaw + SWEEP_DEGREES * (2.0F * progress - 1.0F)) * ((float) Math.PI / 180.0F);
		Vec3 eye = this.bear.getEyePosition();
		this.bear.getLookControl().setLookAt(eye.x - Math.sin(yaw) * 8.0, eye.y - 1.5, eye.z + Math.cos(yaw) * 8.0, 40.0F, 40.0F);
		this.emitCone(level, tick % 4 == 0, tick % 8 == 0);
	}

	/** Frost streams back into the bear's open jaw: the tell. */
	private void inhale(ServerLevel level, int tick) {
		if (tick % 2 != 0) {
			return;
		}
		RandomSource random = this.bear.getRandom();
		Vec3 look = this.bear.getViewVector(1.0F).normalize();
		Vec3 mouth = this.mouth(look);
		for (int i = 0; i < 4; i++) {
			Vec3 from = mouth.add(look.scale(3.0 + random.nextDouble() * 3.0))
					.add((random.nextDouble() - 0.5) * 3.0, (random.nextDouble() - 0.5) * 2.0, (random.nextDouble() - 0.5) * 3.0);
			Vec3 toward = mouth.subtract(from).normalize();
			level.sendParticles(ParticleTypes.SNOWFLAKE, from.x, from.y, from.z, 0, toward.x, toward.y, toward.z, 0.35);
		}
	}

	private Vec3 mouth(Vec3 look) {
		return this.bear.getEyePosition().add(look.scale(1.8)).add(0.0, -0.4, 0.0);
	}

	private void emitCone(ServerLevel level, boolean damageTick, boolean frostbiteTick) {
		RandomSource random = this.bear.getRandom();
		Vec3 look = this.bear.getViewVector(1.0F).normalize();
		Vec3 mouth = this.mouth(look);
		for (int i = 0; i < 6; i++) {
			double dist = 1.0 + random.nextDouble() * RANGE;
			double spread = 0.18 * dist;
			double px = mouth.x + look.x * dist + (random.nextDouble() - 0.5) * spread;
			double py = mouth.y + look.y * dist + (random.nextDouble() - 0.5) * spread;
			double pz = mouth.z + look.z * dist + (random.nextDouble() - 0.5) * spread;
			level.sendParticles(ModParticles.FROST_BREATH.get(), px, py, pz, 1, look.x * 0.2, look.y * 0.2, look.z * 0.2, 0.05);
			level.sendParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 1, 0.0, 0.0, 0.0, 0.01);
		}
		if (!damageTick) {
			return;
		}
		List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class,
				this.bear.getBoundingBox().inflate(RANGE), e -> e != this.bear && !(e instanceof FrostBearEntity) && e.isAlive());
		for (LivingEntity victim : victims) {
			Vec3 to = victim.getEyePosition().subtract(mouth);
			if (to.length() > RANGE || look.dot(to.normalize()) < CONE_COS || !this.bear.hasLineOfSight(victim)) {
				continue;
			}
			victim.hurtServer(level, this.bear.damageSources().freeze(), 2.0F);
			// staying in the whole exhale stacks enough Frostbite to Shatter
			if (frostbiteTick) {
				Frostbite.apply(victim, 1, this.bear);
			}
			victim.push(look.x * 0.1, 0.02, look.z * 0.1);
			victim.hurtMarked = true;
		}
	}
}
