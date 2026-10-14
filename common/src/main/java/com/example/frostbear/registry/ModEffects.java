package com.example.frostbear.registry;

import com.example.frostbear.FrostBear;
import com.example.frostbear.effect.FrostbiteEffect;
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

	/** Stacking cold; see {@link com.example.frostbear.effect.Frostbite}. */
	public static final RegEntry<MobEffect, MobEffect> FROSTBITE = FrostBear.platform().register(
			BuiltInRegistries.MOB_EFFECT, "frostbite",
			key -> new FrostbiteEffect());

	/** Short Frostbite immunity after a Shatter. Its only behavior is that check. */
	public static final RegEntry<MobEffect, MobEffect> THAW = FrostBear.platform().register(
			BuiltInRegistries.MOB_EFFECT, "thaw",
			key -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xFFC27A) {});

	public static void init() {
		FrostBear.LOGGER.info("[FrostBear] Registering status effects");
	}
}
