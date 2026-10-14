package com.example.frostbear.effect;

import com.example.frostbear.FrostBear;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * The Frostbite rules, shared by the FrostBear's attacks and every frost weapon.
 *
 * <p>Each hit adds stacks and refreshes the duration. At {@link #MAX_STACKS} the
 * victim Shatters: it takes {@link #SHATTER_DAMAGE} (credited to the attacker),
 * all stacks clear, and it gains Thaw, which blocks new Frostbite for a few
 * seconds so nothing can be frozen in a loop.
 */
public final class Frostbite {
	public static final int MAX_STACKS = 5;
	public static final int NO_SPRINT_STACKS = 3;
	public static final int DURATION = 120;
	public static final int THAW_DURATION = 80;
	public static final float SHATTER_DAMAGE = 7.0F;

	public static final ResourceKey<DamageType> SHATTER =
			ResourceKey.create(Registries.DAMAGE_TYPE, FrostBear.id("shatter"));

	/** Standing near one of these (or burning) melts Frostbite; see {@link #warm}. */
	public static final TagKey<Block> WARMTH_SOURCES =
			TagKey.create(Registries.BLOCK, FrostBear.id("warmth_sources"));
	public static final int WARMTH_RADIUS = 3;
	public static final int WARMTH_INTERVAL = 30;

	private Frostbite() {}

	/**
	 * Adds {@code stacks} of Frostbite to {@code target}.
	 *
	 * @return true if this pushed the target to {@link #MAX_STACKS} and Shattered it
	 */
	public static boolean apply(LivingEntity target, int stacks, @Nullable Entity attacker) {
		if (stacks <= 0 || !(target.level() instanceof ServerLevel level) || !canAffect(target, attacker)) {
			return false;
		}
		int total = stacks(target) + stacks;
		if (total >= MAX_STACKS) {
			shatter(level, target, attacker);
			return true;
		}
		target.addEffect(new MobEffectInstance(ModEffects.FROSTBITE.holder(), DURATION, total - 1), attacker);
		return false;
	}

	/** Current stack count, 0 when the target has no Frostbite. */
	public static int stacks(LivingEntity target) {
		MobEffectInstance effect = target.getEffect(ModEffects.FROSTBITE.holder());
		return effect == null ? 0 : effect.getAmplifier() + 1;
	}

	/**
	 * Heart of Winter protects against everyone's frost except the FrostBear's own,
	 * so a past kill's reward cannot trivialise the next fight.
	 */
	public static boolean canAffect(LivingEntity target, @Nullable Entity attacker) {
		if (!target.isAlive() || target instanceof FrostBearEntity
				|| target.is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES)
				|| target.hasEffect(ModEffects.THAW.holder())
				|| (target.hasEffect(ModEffects.WINTERS_HEART.holder()) && !(attacker instanceof FrostBearEntity))) {
			return false;
		}
		return !(target instanceof Player player) || !(player.isCreative() || player.isSpectator());
	}

	/** Removes up to {@code stacks}, keeping the rest of the duration. */
	public static void removeStacks(LivingEntity target, int stacks) {
		MobEffectInstance effect = target.getEffect(ModEffects.FROSTBITE.holder());
		if (effect == null || stacks <= 0) {
			return;
		}
		int remaining = effect.getAmplifier() + 1 - stacks;
		target.removeEffect(ModEffects.FROSTBITE.holder());
		if (remaining > 0) {
			target.addEffect(new MobEffectInstance(ModEffects.FROSTBITE.holder(), effect.getDuration(), remaining - 1));
		}
	}

	/** True when the target is burning or within a few blocks of a lit heat source. */
	public static boolean isWarm(ServerLevel level, LivingEntity target) {
		if (target.isOnFire()) {
			return true;
		}
		BlockPos feet = target.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-WARMTH_RADIUS, -1, -WARMTH_RADIUS),
				feet.offset(WARMTH_RADIUS, 2, WARMTH_RADIUS))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(WARMTH_SOURCES) && state.getValueOrElse(BlockStateProperties.LIT, true)) {
				return true;
			}
		}
		return false;
	}

	/** Melts one stack if the target is warm, with steam so players can see it working. */
	public static void warm(ServerLevel level, LivingEntity target) {
		if (stacks(target) == 0 || !isWarm(level, target)) {
			return;
		}
		// Called from Frostbite's own effect tick, while vanilla iterates the target's
		// effects: re-adding the effect now would modify that map mid-iteration.
		MinecraftServer server = level.getServer();
		server.schedule(server.wrapRunnable(() -> {
			if (target.isAlive()) {
				removeStacks(target, 1);
			}
		}));
		level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(0.6), target.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 1.6F);
	}

	private static void shatter(ServerLevel level, LivingEntity target, @Nullable Entity attacker) {
		target.removeEffect(ModEffects.FROSTBITE.holder());
		target.setTicksFrozen(0);
		target.addEffect(new MobEffectInstance(ModEffects.THAW.holder(), THAW_DURATION, 0));

		DamageSource source = new DamageSource(
				level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SHATTER), attacker);
		target.hurtServer(level, source, SHATTER_DAMAGE);

		double x = target.getX();
		double y = target.getY(0.5);
		double z = target.getZ();
		double spread = target.getBbWidth() * 0.5;
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
				x, y, z, 40, spread, target.getBbHeight() * 0.3, spread, 0.15);
		level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 20, spread, 0.4, spread, 0.08);
		level.playSound(null, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.3F, 0.6F);
		level.playSound(null, x, y, z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.0F, 0.8F);
	}
}
