package com.example.frostbear.fabric;

import java.util.function.Function;
import com.example.frostbear.FrostBear;
import com.example.frostbear.platform.FrostBearPlatform;
import com.example.frostbear.platform.RegEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/** Fabric registries are open during mod init, so objects are registered immediately. */
final class FabricPlatform implements FrostBearPlatform {
	@Override
	public <R, T extends R> RegEntry<R, T> register(Registry<R> registry, String name,
			Function<ResourceKey<R>, T> factory) {
		ResourceKey<R> key = ResourceKey.create(registry.key(), FrostBear.id(name));
		T value = factory.apply(key);
		Holder.Reference<R> holder = Registry.registerForHolder(registry, key, value);
		return new RegEntry<>(key, () -> value, () -> holder);
	}
}
