package com.example.frostbear.entity.goal;

import com.example.frostbear.effect.Frostbite;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.FrostBearMove;
import com.example.frostbear.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Rears up and bellows: after a one-second wind-up, a blast of frost slows and chills
 * every player in sight within nine blocks and shoves them back. Break line of sight
 * or get out of range during the wind-up to avoid it.
 */
public class FrostBearRoarGoal extends FrostBearAttackGoal {
	private static final double RANGE = 9.0;

	public FrostBearRoarGoal(FrostBearEntity bear) {
		super(bear, FrostBearMove.ROAR, 120, 240, 120);
	}

	@Override
	protected boolean canStart(LivingEntity target) {
		return this.bear.onGround() && this.distanceTo(target) < 14.0;
	}

	@Override
	protected void onStart(ServerLevel level) {
		this.bear.playSound(ModSounds.FROST_BEAR_ROAR.get(), 2.4F, 1.0F);
	}

	@Override
	protected void onTick(ServerLevel level, FrostBearMove move, int tick) {
		if (tick != FrostBearMove.ROAR_BLAST) {
			return;
		}
		for (int i = 0; i < 40; i++) {
			double angle = Math.PI * 2.0 * i / 40.0;
			double radius = 2.0 + (i % 3) * 2.8;
			level.sendParticles(ParticleTypes.SNOWFLAKE,
					this.bear.getX() + Math.cos(angle) * radius, this.bear.getY(0.55),
					this.bear.getZ() + Math.sin(angle) * radius, 2, 0.08, 0.18, 0.08, 0.04);
		}
		level.sendParticles(ParticleTypes.POOF, this.bear.getX(), this.bear.getY(0.5), this.bear.getZ(), 24, 1.7, 1.0, 1.7, 0.09);
		for (Player player : this.bear.nearbyPlayers(level, RANGE)) {
			if (!this.bear.hasLineOfSight(player)) {
				continue;
			}
			player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 80, 1));
			Frostbite.apply(player, 1, this.bear);
			player.knockback(0.8, this.bear.getX() - player.getX(), this.bear.getZ() - player.getZ(),
					this.bear.damageSources().mobAttack(this.bear), 0.0F);
		}
	}
}
