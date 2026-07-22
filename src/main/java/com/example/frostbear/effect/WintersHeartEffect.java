package com.example.frostbear.effect;

import java.util.List;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

/**
 * "Heart of Winter" — the boss-drop buff.
 *
 * <p>While active, the holder is immune to freezing and radiates the FrostBear's
 * own chill: every half-second, nearby hostile mobs are slowed and frost builds
 * on them (the powder-snow freeze overlay). Turn the boss's power against the world.
 */
public class WintersHeartEffect extends MobEffect {
	public WintersHeartEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {

			// stay thawed
			entity.setTicksFrozen(0);

			double radius = 5.0 + amplifier;
			List<Monster> mobs = level.getEntitiesOfClass(Monster.class,
					entity.getBoundingBox().inflate(radius),
					m -> m != entity && m.isAlive());
			for (Monster mob : mobs) {
				mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1 + amplifier));
				mob.setTicksFrozen(Math.min(300, mob.getTicksFrozen() + 40));
			}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % 10 == 0; // ~twice per second
	}
}
