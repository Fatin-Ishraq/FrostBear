package com.example.frostbear.registry;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.LongStream;
import com.example.frostbear.FrostBear;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Persists which igloo chunks have already received a FrostBear guardian. */
public class IglooState extends SavedData {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(FrostBear.MOD_ID, "igloos_v2");

	public static final Codec<IglooState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG_STREAM.fieldOf("Handled").forGetter(IglooState::handledStream))
			.apply(instance, IglooState::new));

	public static final SavedDataType<IglooState> TYPE =
			new SavedDataType<>(ID, IglooState::new, CODEC, null);

	private final Set<Long> handled = new HashSet<>();

	public IglooState() {
	}

	private IglooState(LongStream handled) {
		handled.forEach(this.handled::add);
	}

	private LongStream handledStream() {
		return this.handled.stream().mapToLong(Long::longValue);
	}

	public static IglooState get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean claim(long chunkKey) {
		if (!this.handled.add(chunkKey)) {
			return false;
		}
		this.setDirty();
		return true;
	}

	public boolean isHandled(long chunkKey) {
		return this.handled.contains(chunkKey);
	}
}