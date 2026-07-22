package com.example.frostbear.registry;

import com.example.frostbear.FrostBear;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	private ModSounds() {}

	public static final SoundEvent FROST_BEAR_ROAR   = reg("frost_bear_roar");
	public static final SoundEvent FROST_BEAR_GROWL  = reg("frost_bear_growl");
	public static final SoundEvent FROST_BEAR_HURT   = reg("frost_bear_hurt");
	public static final SoundEvent FROST_BEAR_DEATH  = reg("frost_bear_death");
	public static final SoundEvent FROST_BEAR_STEP   = reg("frost_bear_step");
	public static final SoundEvent FROST_BEAR_BREATH = reg("frost_bear_breath");
	public static final SoundEvent FROST_BEAR_CHARGE = reg("frost_bear_charge");

	private static SoundEvent reg(String name) {
		Identifier id = Identifier.fromNamespaceAndPath(FrostBear.MOD_ID, name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void register() {
		FrostBear.LOGGER.info("[FrostBear] Registering sounds");
	}
}
