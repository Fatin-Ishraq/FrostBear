package com.example.frostbear.registry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import com.example.frostbear.FrostBear;
import com.example.frostbear.entity.FrostBearEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Makes every igloo a FrostBear den: the first time an igloo is ever loaded, a
 * single guardian is spawned there. The igloo is then recorded in {@link IglooState}
 * (saved with the world), so reloading the chunk never spawns another one — kill
 * the guardian and the den stays empty.
 *
 * <p>Each loader module forwards its chunk-load and end-of-level-tick events to
 * {@link #onChunkLoad} and {@link #onLevelTick}.
 */
public final class IglooSpawns {
	private static final Map<ServerLevel, Map<Long, PendingIgloo>> PENDING = new WeakHashMap<>();

	private IglooSpawns() {}

	public static void onChunkLoad(ServerLevel world, ChunkAccess chunk) {
		Registry<Structure> structures = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		Structure igloo = structures.getValue(BuiltinStructures.IGLOO);
		if (igloo == null) {
			return;
		}

		StructureStart start = chunk.getStartForStructure(igloo);
		if (start == null || !start.isValid()) {
			return;
		}

		BoundingBox box = start.getBoundingBox();
		BlockPos center = box.getCenter();
		long key = chunk.getPos().pack();

		// Chunk load may run while the chunk is still being promoted. Queue only
		// lightweight coordinates on the server thread and spawn after a level tick.
		world.getServer().execute(() -> PENDING
				.computeIfAbsent(world, ignored -> new HashMap<>())
				.putIfAbsent(key, new PendingIgloo(key, center.getX(), center.getZ())));
	}

	/** Spawns at most one newly discovered guardian per level per tick. */
	public static void onLevelTick(ServerLevel world) {
		Map<Long, PendingIgloo> pending = PENDING.get(world);
		if (pending == null || pending.isEmpty()) {
			return;
		}

		Iterator<PendingIgloo> iterator = pending.values().iterator();
		while (iterator.hasNext()) {
			PendingIgloo den = iterator.next();
			int chunkX = den.x() >> 4;
			int chunkZ = den.z() >> 4;
			if (!world.getChunkSource().hasChunk(chunkX, chunkZ)) {
				continue;
			}

			IglooState state = IglooState.get(world);
			if (state.isHandled(den.key())) {
				iterator.remove();
				return;
			}

			int y = world.getHeight(Heightmap.Types.WORLD_SURFACE, den.x(), den.z());
			FrostBearEntity bear = ModEntities.FROST_BEAR.get().create(world, EntitySpawnReason.STRUCTURE);
			if (bear == null) {
				iterator.remove();
				FrostBear.LOGGER.error("[FrostBear] Could not create igloo guardian at {} {}", den.x(), den.z());
				return;
			}

			bear.snapTo(
					den.x() + 0.5, y, den.z() + 0.5,
					world.getRandom().nextFloat() * 360.0f, 0.0f);
			bear.setPersistenceRequired();
			if (world.addFreshEntity(bear)) {
				state.claim(den.key());
				FrostBear.LOGGER.info("[FrostBear] Igloo guardian spawned at {} {} {}", den.x(), y, den.z());
			} else {
				FrostBear.LOGGER.warn("[FrostBear] Igloo guardian spawn was rejected at {} {} {}", den.x(), y, den.z());
			}
			iterator.remove();
			return;
		}
	}

	private record PendingIgloo(long key, int x, int z) {}
}
