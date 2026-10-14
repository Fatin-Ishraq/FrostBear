package com.example.frostbear.gametest;

import com.example.frostbear.FrostBear;
import com.example.frostbear.effect.Frostbite;
import com.example.frostbear.registry.ModEffects;
import com.example.frostbear.registry.ModEntities;
import com.example.frostbear.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.FrostBearMove;
import com.example.frostbear.entity.IceSpikeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Game tests shared by both loaders. The Fabric and NeoForge modules register
 * each method as a test on {@link #PLATFORM}; neither loader registers them
 * outside a development environment.
 */
public final class FrostBearGameTests {
	/** 5x4x5 stone-floored box from data/frostbear/structure/gametest/platform.nbt. */
	public static final Identifier PLATFORM = FrostBear.id("gametest/platform");
	public static final int MAX_TICKS = 100;
	/** For tests that let the AI choose its moves. */
	public static final int AI_MAX_TICKS = 400;

	private static final BlockPos MOB_POS = new BlockPos(2, 1, 2);

	private FrostBearGameTests() {}

	public static void frostbiteStacksThenShatters(GameTestHelper helper) {
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, MOB_POS);
		float healthBefore = zombie.getHealth();

		for (int i = 1; i < Frostbite.MAX_STACKS; i++) {
			helper.assertFalse(Frostbite.apply(zombie, 1, null), "Shattered after only " + i + " stacks");
			helper.assertTrue(Frostbite.stacks(zombie) == i, "Expected " + i + " stacks, got " + Frostbite.stacks(zombie));
		}
		helper.assertTrue(zombie.getHealth() == healthBefore, "Stacking alone should not deal damage");

		helper.assertTrue(Frostbite.apply(zombie, 1, null), "The final stack should Shatter");
		helper.assertTrue(Frostbite.stacks(zombie) == 0, "Shatter should clear Frostbite");
		helper.assertTrue(zombie.hasEffect(ModEffects.THAW.holder()), "Shatter should grant Thaw");
		helper.assertTrue(zombie.getHealth() < healthBefore, "Shatter should deal damage");
		helper.succeed();
	}

	public static void thawBlocksFrostbite(GameTestHelper helper) {
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, MOB_POS);
		Frostbite.apply(zombie, Frostbite.MAX_STACKS, null);

		helper.assertFalse(Frostbite.apply(zombie, 1, null), "A Thawed target cannot Shatter again");
		helper.assertTrue(Frostbite.stacks(zombie) == 0, "Thaw should block new Frostbite");
		helper.succeed();
	}

	public static void frostbiteSlowsMovement(GameTestHelper helper) {
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, MOB_POS);
		double speedBefore = zombie.getAttributeValue(Attributes.MOVEMENT_SPEED);

		Frostbite.apply(zombie, 2, null);
		double expected = speedBefore * (1.0 - 0.08 * 2);
		double actual = zombie.getAttributeValue(Attributes.MOVEMENT_SPEED);
		helper.assertTrue(Math.abs(actual - expected) < 1.0e-6,
				"Two stacks should slow by 16%: expected " + expected + ", got " + actual);
		helper.succeed();
	}

	public static void frostImmuneTargetsIgnoreFrostbite(GameTestHelper helper) {
		Mob stray = helper.spawnWithNoFreeWill(EntityTypes.STRAY, MOB_POS);
		Mob bear = helper.spawnWithNoFreeWill(ModEntities.FROST_BEAR.get(), MOB_POS);
		Mob warded = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, MOB_POS);
		warded.addEffect(new MobEffectInstance(ModEffects.WINTERS_HEART.holder(), 200));

		for (Mob mob : new Mob[] {stray, bear, warded}) {
			Frostbite.apply(mob, 1, null);
			helper.assertTrue(Frostbite.stacks(mob) == 0,
					mob.getType().getDescriptionId() + " should be immune to Frostbite");
		}
		helper.succeed();
	}

	public static void frostfangBonusNeedsFrostbite(GameTestHelper helper) {
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, MOB_POS);
		var source = helper.getLevel().damageSources().generic();

		float before = ModItems.FROSTFANG.get().getAttackDamageBonus(zombie, 10.0F, source);
		Frostbite.apply(zombie, 1, null);
		float after = ModItems.FROSTFANG.get().getAttackDamageBonus(zombie, 10.0F, source);

		helper.assertTrue(before == 0.0F, "No bonus before Frostbite, got " + before);
		helper.assertTrue(Math.abs(after - 2.0F) < 1.0e-6, "Expected +20% (2.0) on a Frostbitten target, got " + after);
		helper.succeed();
	}

	// ------------------------------------------------------------------------------------
	// Boss fight rules
	// ------------------------------------------------------------------------------------

	private static final float PROBE_DAMAGE = 10.0F;

	/** A boss facing +Z with no AI, so only the rules under test act on it. */
	private static FrostBearEntity spawnBoss(GameTestHelper helper) {
		FrostBearEntity bear = helper.spawnWithNoFreeWill(ModEntities.FROST_BEAR.get(), MOB_POS);
		bear.setYRot(0.0F);
		bear.yBodyRot = 0.0F;
		bear.yHeadRot = 0.0F;
		return bear;
	}

	/** Generic damage (armour-free, so the numbers are exact) coming from a point beside the bear. */
	private static float hitFrom(GameTestHelper helper, FrostBearEntity bear, double dz) {
		ServerLevel level = helper.getLevel();
		bear.invulnerableTime = 0;
		float before = bear.getHealth();
		Vec3 from = bear.position().add(0.0, 1.0, dz);
		bear.hurtServer(level, new DamageSource(level.damageSources().generic().typeHolder(), from), PROBE_DAMAGE);
		return before - bear.getHealth();
	}

	public static void frontalGuardReducesFrontDamage(GameTestHelper helper) {
		FrostBearEntity bear = spawnBoss(helper);
		float front = hitFrom(helper, bear, 3.0);
		float back = hitFrom(helper, bear, -3.0);
		helper.assertTrue(Math.abs(back - PROBE_DAMAGE) < 1.0e-3, "A hit from behind should deal full damage, dealt " + back);
		helper.assertTrue(Math.abs(front - PROBE_DAMAGE * (1.0F - FrostBearEntity.GUARD_REDUCTION)) < 1.0e-3,
				"A hit from the front should be reduced by the guard, dealt " + front);
		helper.succeed();
	}

	public static void fireThawsTheGuard(GameTestHelper helper) {
		FrostBearEntity bear = spawnBoss(helper);
		bear.igniteForSeconds(2.0F); // Fire Aspect and Flame arrows do this
		helper.assertFalse(bear.isGuardUp(), "Fire should thaw the guard");
		helper.assertTrue(bear.getRemainingFireTicks() == 0, "The bear itself should never burn");
		float front = hitFrom(helper, bear, 3.0);
		helper.assertTrue(Math.abs(front - PROBE_DAMAGE) < 1.0e-3, "A thawed guard should not reduce damage, dealt " + front);
		helper.succeed();
	}

	public static void stunDoublesDamage(GameTestHelper helper) {
		FrostBearEntity bear = spawnBoss(helper);
		bear.stun(helper.getLevel(), FrostBearEntity.CHARGE_STUN_TICKS);
		float front = hitFrom(helper, bear, 3.0);
		helper.assertTrue(Math.abs(front - PROBE_DAMAGE * FrostBearEntity.STUN_DAMAGE_MULTIPLIER) < 1.0e-3,
				"A stunned bear has no guard and takes double damage, dealt " + front);
		helper.succeed();
	}

	public static void phaseTwoBeginsAtHalfHealth(GameTestHelper helper) {
		FrostBearEntity bear = spawnBoss(helper);
		bear.setHealth(bear.getMaxHealth() * 0.51F);
		bear.checkPhase(helper.getLevel());
		helper.assertFalse(bear.isEnraged(), "Still phase 1 above half health");
		bear.setHealth(bear.getMaxHealth() * 0.49F);
		bear.checkPhase(helper.getLevel());
		helper.assertTrue(bear.isEnraged(), "Phase 2 at half health");
		helper.assertTrue(bear.getMove() == FrostBearMove.TRANSITION, "Phase 2 opens with the transition roar");
		float health = bear.getHealth();
		hitFrom(helper, bear, -3.0);
		helper.assertTrue(bear.getHealth() == health, "The bear cannot be hurt during the transition roar");
		helper.succeed();
	}

	public static void leashResetsAnAbandonedFight(GameTestHelper helper) {
		FrostBearEntity bear = spawnBoss(helper);
		bear.setHealth(bear.getMaxHealth() * 0.49F);
		bear.checkPhase(helper.getLevel());
		for (int i = 0; i < FrostBearEntity.LEASH_TICKS; i++) {
			bear.tickLeash(helper.getLevel()); // the test world has no players nearby
		}
		helper.assertTrue(bear.getHealth() == bear.getMaxHealth(), "An abandoned fight heals the bear fully");
		helper.assertFalse(bear.isEnraged(), "An abandoned fight resets to phase 1");
		helper.succeed();
	}

	public static void bearFrostPiercesHeartOfWinter(GameTestHelper helper) {
		FrostBearEntity bear = spawnBoss(helper);
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(0, 1, 0));
		zombie.addEffect(new MobEffectInstance(ModEffects.WINTERS_HEART.holder(), 200));
		Frostbite.apply(zombie, 1, null);
		helper.assertTrue(Frostbite.stacks(zombie) == 0, "Heart of Winter blocks ordinary frost");
		Frostbite.apply(zombie, 1, bear);
		helper.assertTrue(Frostbite.stacks(zombie) == 1, "Heart of Winter does not block the FrostBear's own frost");
		helper.succeed();
	}

	public static void bossShrugsOffSlowness(GameTestHelper helper) {
		FrostBearEntity bear = spawnBoss(helper);
		bear.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200));
		bear.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
		MobEffectInstance slowness = bear.getEffect(MobEffects.SLOWNESS);
		MobEffectInstance weakness = bear.getEffect(MobEffects.WEAKNESS);
		helper.assertTrue(slowness != null && slowness.getDuration() == 100, "Slowness should last half as long");
		helper.assertTrue(weakness != null && weakness.getDuration() == 100, "Weakness should last half as long");
		helper.succeed();
	}

	public static void warmthMeltsFrostbite(GameTestHelper helper) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 1, 1));
		helper.setBlock(new BlockPos(3, 1, 1), Blocks.CAMPFIRE);
		Frostbite.apply(pig, 3, null);
		helper.succeedWhen(() -> helper.assertTrue(Frostbite.stacks(pig) < 3,
				"A lit campfire nearby should melt Frostbite, still at " + Frostbite.stacks(pig)));
	}

	public static void slamShattersOnlySoftBlocks(GameTestHelper helper) {
		FrostBearEntity bear = spawnBoss(helper);
		BlockPos dirt = new BlockPos(0, 1, 4);
		BlockPos stone = new BlockPos(4, 1, 4);
		helper.setBlock(dirt, Blocks.DIRT);
		helper.setBlock(stone, Blocks.STONE);
		bear.smashSoftBlocks(helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 4)), helper.absolutePos(new BlockPos(4, 1, 4)), 10);
		helper.assertTrue(helper.getBlockState(dirt).isAir(), "Soft blocks like dirt should shatter");
		helper.assertTrue(helper.getBlockState(stone).is(Blocks.STONE), "Hard blocks like stone should hold");
		helper.succeed();
	}

	public static void spikesNeverEntombAnything(GameTestHelper helper) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, MOB_POS);
		IceSpikeEntity.spawnSpike(helper.getLevel(), pig.getX(), pig.getY(), pig.getZ(), 3, 0, null);
		helper.runAfterDelay(8, () -> {
			BlockPos at = pig.blockPosition();
			helper.assertFalse(helper.getLevel().getBlockState(at).is(Blocks.PACKED_ICE)
					|| helper.getLevel().getBlockState(at).is(Blocks.ICE), "A spike must not seal a mob inside ice");
			helper.succeed();
		});
	}

	// ------------------------------------------------------------------------------------
	// AI smoke tests: a target with no AI stands in for a player (mock players have no
	// network connection, which the boss bar needs)
	// ------------------------------------------------------------------------------------

	public static void aiMaulsATargetInReach(GameTestHelper helper) {
		Mob dummy = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(2, 1, 4));
		FrostBearEntity bear = helper.spawn(ModEntities.FROST_BEAR.get(), new BlockPos(2, 1, 1));
		bear.setTarget(dummy);
		float full = dummy.getHealth();
		boolean[] mauled = {false};
		helper.onEachTick(() -> mauled[0] |= bear.getMove() == FrostBearMove.MAUL);
		helper.succeedWhen(() -> {
			helper.assertTrue(mauled[0], "The bear should open a maul on a target in reach");
			helper.assertTrue(dummy.getHealth() < full || !dummy.isAlive(), "The maul should land");
		});
	}

	public static void aiAnswersATargetOnAPillar(GameTestHelper helper) {
		for (int y = 1; y <= 4; y++) {
			helper.setBlock(new BlockPos(4, y, 4), Blocks.STONE);
		}
		Mob dummy = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(4, 5, 4));
		FrostBearEntity bear = helper.spawn(ModEntities.FROST_BEAR.get(), new BlockPos(1, 1, 1));
		bear.setTarget(dummy);
		helper.succeedWhen(() -> {
			bear.setTarget(dummy); // keep it locked on the perched target
			Vec3 top = dummy.position();
			boolean column = !helper.getLevel().getEntitiesOfClass(IceSpikeEntity.class,
					new net.minecraft.world.phys.AABB(top, top).inflate(0.8, 0.6, 0.8)).isEmpty();
			helper.assertTrue(column, "The bear should erupt a spike column on top of the pillar");
		});
	}
}
