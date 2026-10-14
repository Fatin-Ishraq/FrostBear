package com.example.frostbear.platform;

import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

/** A registered object, usable as either its value or its registry holder. */
public final class RegEntry<R, T extends R> implements Supplier<T> {
	private final ResourceKey<R> key;
	private final Supplier<T> value;
	private final Supplier<? extends Holder<R>> holder;

	public RegEntry(ResourceKey<R> key, Supplier<T> value, Supplier<? extends Holder<R>> holder) {
		this.key = key;
		this.value = value;
		this.holder = holder;
	}

	@Override
	public T get() {
		return this.value.get();
	}

	public Holder<R> holder() {
		return this.holder.get();
	}

	public ResourceKey<R> key() {
		return this.key;
	}
}
