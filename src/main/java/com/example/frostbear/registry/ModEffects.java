package com.example.frostbear.registry;

import com.example.frostbear.FrostBear;
import com.example.frostbear.effect.WintersHeartEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class ModEffects {
	private ModEffects() {}

	/** "Heart of Winter" — granted by devouring a Frozen Heart. */
	public static final Holder<MobEffect> WINTERS_HEART = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			Identifier.fromNamespaceAndPath(FrostBear.MOD_ID, "winters_heart"),
			new WintersHeartEffect(MobEffectCategory.BENEFICIAL, 0x9FE6FF));

	public static void register() {
		FrostBear.LOGGER.info("[FrostBear] Registering status effects");
	}
}
