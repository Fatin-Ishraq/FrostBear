package com.example.frostbear.registry;

import com.example.frostbear.FrostBear;
import com.example.frostbear.platform.RegEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	private ModSounds() {}

	public static final RegEntry<SoundEvent, SoundEvent> FROST_BEAR_ROAR   = register("frost_bear_roar");
	public static final RegEntry<SoundEvent, SoundEvent> FROST_BEAR_GROWL  = register("frost_bear_growl");
	public static final RegEntry<SoundEvent, SoundEvent> FROST_BEAR_HURT   = register("frost_bear_hurt");
	public static final RegEntry<SoundEvent, SoundEvent> FROST_BEAR_DEATH  = register("frost_bear_death");
	public static final RegEntry<SoundEvent, SoundEvent> FROST_BEAR_STEP   = register("frost_bear_step");
	public static final RegEntry<SoundEvent, SoundEvent> FROST_BEAR_BREATH = register("frost_bear_breath");
	public static final RegEntry<SoundEvent, SoundEvent> FROST_BEAR_CHARGE = register("frost_bear_charge");

	private static RegEntry<SoundEvent, SoundEvent> register(String name) {
		return FrostBear.platform().register(BuiltInRegistries.SOUND_EVENT, name,
				key -> SoundEvent.createVariableRangeEvent(FrostBear.id(name)));
	}

	public static void init() {
		FrostBear.LOGGER.info("[FrostBear] Registering sounds");
	}
}
