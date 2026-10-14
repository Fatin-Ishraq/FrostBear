package com.example.frostbear.neoforge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import com.example.frostbear.FrostBear;
import com.example.frostbear.platform.FrostBearPlatform;
import com.example.frostbear.platform.RegEntry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Collects one DeferredRegister per vanilla registry and attaches them all to the mod bus. */
final class NeoForgePlatform implements FrostBearPlatform {
	private final Map<ResourceKey<?>, DeferredRegister<?>> registers = new LinkedHashMap<>();

	@Override
	@SuppressWarnings("unchecked")
	public <R, T extends R> RegEntry<R, T> register(Registry<R> registry, String name,
			Function<ResourceKey<R>, T> factory) {
		DeferredRegister<R> register = (DeferredRegister<R>) this.registers.computeIfAbsent(registry.key(),
				ignored -> DeferredRegister.create(registry, FrostBear.MOD_ID));
		DeferredHolder<R, T> holder = register.register(name,
				id -> factory.apply(ResourceKey.create(registry.key(), id)));
		return new RegEntry<>(holder.getKey(), holder, () -> holder);
	}

	void registerAll(IEventBus modBus) {
		this.registers.values().forEach(register -> register.register(modBus));
	}
}
