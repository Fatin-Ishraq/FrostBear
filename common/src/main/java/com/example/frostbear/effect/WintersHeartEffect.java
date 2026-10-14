package com.example.frostbear.effect;

import java.util.List;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

/**
 * "Heart of Winter" — the boss-drop buff.
 *
 * <p>While active, the holder is immune to freezing and to Frostbite, and radiates
 * the FrostBear's own chill: every two seconds, each nearby hostile mob gains a
 * Frostbite stack, so anything that stays close eventually Shatters. Turn the
 * boss's power against the world.
 */
public class WintersHeartEffect extends MobEffect {
	private static final int TICK_INTERVAL = 10;
	/** Aura pulses every AURA_EVERY effect ticks (every 2 seconds). */
	private static final int AURA_EVERY = 4;

	public WintersHeartEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		// stay thawed
		entity.setTicksFrozen(0);

		// effect ticks arrive every TICK_INTERVAL game ticks, so this advances by one per call
		if ((entity.tickCount / TICK_INTERVAL) % AURA_EVERY != 0) {
			return true;
		}
		double radius = 5.0 + amplifier;
		List<Monster> mobs = level.getEntitiesOfClass(Monster.class,
				entity.getBoundingBox().inflate(radius),
				m -> m != entity && m.isAlive());
		for (Monster mob : mobs) {
			Frostbite.apply(mob, 1, entity);
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % TICK_INTERVAL == 0;
	}
}
