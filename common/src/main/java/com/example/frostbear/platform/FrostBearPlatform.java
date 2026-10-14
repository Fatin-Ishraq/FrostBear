package com.example.frostbear.platform;

import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * The loader-specific services common code needs. Fabric and NeoForge each pass
 * an implementation to {@link com.example.frostbear.FrostBear#init}.
 */
public interface FrostBearPlatform {
	/**
	 * Registers {@code name} in {@code registry}. Fabric registers immediately;
	 * NeoForge defers until its registry event, so call {@link RegEntry#get()}
	 * only after registration has finished.
	 */
	<R, T extends R> RegEntry<R, T> register(Registry<R> registry, String name, Function<ResourceKey<R>, T> factory);
}
