package com.example.frostbear.registry;

import com.example.frostbear.FrostBear;
import com.example.frostbear.effect.WintersHeartEffect;
import com.example.frostbear.platform.RegEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class ModEffects {
	private ModEffects() {}

	/** "Heart of Winter" — granted by the Frozen Heart. */
	public static final RegEntry<MobEffect, MobEffect> WINTERS_HEART = FrostBear.platform().register(
			BuiltInRegistries.MOB_EFFECT, "winters_heart",
			key -> new WintersHeartEffect(MobEffectCategory.BENEFICIAL, 0x9FE6FF));

	public static void init() {
		FrostBear.LOGGER.info("[FrostBear] Registering status effects");
	}
}
