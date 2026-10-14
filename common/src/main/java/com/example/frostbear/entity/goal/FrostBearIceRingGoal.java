package com.example.frostbear.entity.goal;

import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.FrostBearMove;
import com.example.frostbear.entity.IceSpikeEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 2 Ice Ring, which punishes hugging the bear. It hunches while frost rises from
 * the ground around it, then two rings of spikes burst outward, the outer one a beat
 * later. Back off during the hunch, or slip between the outer spikes.
 */
public class FrostBearIceRingGoal extends FrostBearAttackGoal {
	private static final double INNER = 2.8;
	private static final double OUTER = 4.6;

	public FrostBearIceRingGoal(FrostBearEntity bear) {
		super(bear, FrostBearMove.ICE_RING, 60, 180, 80);
	}

	@Override
	protected boolean canStart(LivingEntity target) {
		return this.bear.isEnraged() && this.bear.onGround() && this.distanceTo(target) <= 5.5;
	}

	@Override
	protected void onStart(ServerLevel level) {
		level.playSound(null, this.bear.getX(), this.bear.getY(), this.bear.getZ(),
				SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2.0F, 0.5F);
	}

	@Override
	protected void onTick(ServerLevel level, FrostBearMove move, int tick) {
		Vec3 center = this.bear.position();
		if (tick < FrostBearMove.RING_BURST) {
			if (tick % 3 == 0) {
				for (int i = 0; i < 20; i++) {
					double angle = Math.PI * 2.0 * i / 20.0 + tick * 0.1;
					double radius = i % 2 == 0 ? INNER : OUTER;
					level.sendParticles(ParticleTypes.SNOWFLAKE, center.x + Math.cos(angle) * radius, center.y + 0.1,
							center.z + Math.sin(angle) * radius, 0, 0.0, 1.0, 0.0, 0.08);
				}
			}
			return;
		}
		if (tick == FrostBearMove.RING_BURST) {
			IceSpikeEntity.spawnRing(level, center, INNER, 10, 0, this.bear);
			IceSpikeEntity.spawnRing(level, center, OUTER, 14, 5, this.bear);
			level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.7F);
		}
	}
}
