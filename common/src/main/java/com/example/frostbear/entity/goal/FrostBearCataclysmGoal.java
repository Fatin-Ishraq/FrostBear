package com.example.frostbear.entity.goal;

import com.example.frostbear.effect.Frostbite;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.FrostBearMove;
import com.example.frostbear.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Glacial Cataclysm, the phase 2 signature. The bear plants itself for three seconds:
 * the sky darkens, the boss bar turns white, and frost spirals inward with a rising
 * chime. On release, every player it can see within range takes heavy Frostbite.
 *
 * <p>Two answers: get behind solid blocks, or break the channel by dealing
 * {@link FrostBearEntity#CATACLYSM_INTERRUPT_DAMAGE} damage (or any fire) during it,
 * which stuns the bear. Never used on Easy.
 */
public class FrostBearCataclysmGoal extends FrostBearAttackGoal {
	private static final double RANGE = 24.0;
	private static final double TRIGGER_RANGE = 20.0;
	private static final float DAMAGE = 6.0F;
	private static final int FROSTBITE = 4;
	private static final int FIRST_DELAY_AFTER_PHASE_TWO = 300;

	public FrostBearCataclysmGoal(FrostBearEntity bear) {
		super(bear, FrostBearMove.CATACLYSM, 0, 1100, 0);
	}

	@Override
	public boolean canUse() {
		if (!this.bear.isEnraged() || this.bear.level().getDifficulty() == Difficulty.EASY) {
			this.delay(FIRST_DELAY_AFTER_PHASE_TWO); // first one comes a while after the transition
			return false;
		}
		return super.canUse();
	}

	@Override
	protected boolean canStart(LivingEntity target) {
		return this.bear.onGround() && this.bear.level() instanceof ServerLevel level
				&& !this.bear.nearbyPlayers(level, TRIGGER_RANGE).isEmpty();
	}

	@Override
	protected void onStart(ServerLevel level) {
		this.bear.beginCataclysm();
		level.playSound(null, this.bear.getX(), this.bear.getY(), this.bear.getZ(),
				SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3.0F, 0.5F);
		this.bear.playSound(ModSounds.FROST_BEAR_ROAR.get(), 2.6F, 0.6F);
	}

	@Override
	protected boolean lookAtTarget(FrostBearMove move, int tick) {
		return tick < FrostBearMove.CATACLYSM_RELEASE;
	}

	@Override
	protected void onTick(ServerLevel level, FrostBearMove move, int tick) {
		if (tick < FrostBearMove.CATACLYSM_RELEASE) {
			this.channel(level, tick);
		} else if (tick == FrostBearMove.CATACLYSM_RELEASE) {
			this.bear.endCataclysm();
			this.release(level);
		}
	}

	/** Frost spirals inward and a chime climbs in pitch: the countdown. */
	private void channel(ServerLevel level, int tick) {
		float progress = tick / (float) FrostBearMove.CATACLYSM_RELEASE;
		if (tick % 10 == 0) {
			level.playSound(null, this.bear.getX(), this.bear.getY(), this.bear.getZ(),
					SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2.5F, 0.5F + progress);
		}
		Vec3 core = this.bear.position().add(0.0, 2.0, 0.0);
		double radius = 9.0 - 7.0 * progress;
		for (int i = 0; i < 8; i++) {
			double angle = Math.PI * 2.0 * i / 8.0 + tick * 0.25;
			Vec3 from = core.add(Math.cos(angle) * radius, -1.0 + (i % 3), Math.sin(angle) * radius);
			Vec3 toward = core.subtract(from).normalize();
			level.sendParticles(ParticleTypes.SNOWFLAKE, from.x, from.y, from.z, 0, toward.x, toward.y, toward.z, 0.4);
		}
	}

	private void release(ServerLevel level) {
		Vec3 core = this.bear.position().add(0.0, 2.0, 0.0);
		for (Player player : this.bear.nearbyPlayers(level, RANGE)) {
			Vec3 eye = player.getEyePosition();
			HitResult cover = level.clip(new ClipContext(core, eye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.bear));
			if (cover.getType() == HitResult.Type.MISS) {
				player.hurtServer(level, this.bear.damageSources().freeze(), DAMAGE);
				Frostbite.apply(player, FROSTBITE, this.bear);
				level.sendParticles(ParticleTypes.SNOWFLAKE, eye.x, eye.y - 0.5, eye.z, 30, 0.4, 0.6, 0.4, 0.05);
			} else {
				Vec3 at = cover.getLocation();
				level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 20, 0.3, 0.3, 0.3, 0.08); // frost breaks on the cover
			}
		}
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()),
				core.x, core.y, core.z, 120, 3.0, 1.5, 3.0, 0.4);
		level.sendParticles(ParticleTypes.SNOWFLAKE, core.x, core.y, core.z, 120, 6.0, 2.0, 6.0, 0.3);
		level.playSound(null, core.x, core.y, core.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0F, 0.4F);
		this.bear.playSound(ModSounds.FROST_BEAR_ROAR.get(), 3.0F, 0.55F);
	}

	@Override
	protected void onStop() {
		this.bear.endCataclysm();
	}
}
