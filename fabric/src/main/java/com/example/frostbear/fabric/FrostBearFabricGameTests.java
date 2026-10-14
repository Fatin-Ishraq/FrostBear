package com.example.frostbear.fabric;

import com.example.frostbear.gametest.FrostBearGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Registers the shared {@link FrostBearGameTests} with Fabric's game test API
 * (the "fabric-gametest" entrypoint, loaded only in development environments).
 */
public class FrostBearFabricGameTests {
	private static final String PLATFORM = "frostbear:gametest/platform";

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void frostbiteStacksThenShatters(GameTestHelper helper) {
		FrostBearGameTests.frostbiteStacksThenShatters(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void thawBlocksFrostbite(GameTestHelper helper) {
		FrostBearGameTests.thawBlocksFrostbite(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void frostbiteSlowsMovement(GameTestHelper helper) {
		FrostBearGameTests.frostbiteSlowsMovement(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void frostImmuneTargetsIgnoreFrostbite(GameTestHelper helper) {
		FrostBearGameTests.frostImmuneTargetsIgnoreFrostbite(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void frostfangBonusNeedsFrostbite(GameTestHelper helper) {
		FrostBearGameTests.frostfangBonusNeedsFrostbite(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void frontalGuardReducesFrontDamage(GameTestHelper helper) {
		FrostBearGameTests.frontalGuardReducesFrontDamage(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void fireThawsTheGuard(GameTestHelper helper) {
		FrostBearGameTests.fireThawsTheGuard(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void stunDoublesDamage(GameTestHelper helper) {
		FrostBearGameTests.stunDoublesDamage(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void phaseTwoBeginsAtHalfHealth(GameTestHelper helper) {
		FrostBearGameTests.phaseTwoBeginsAtHalfHealth(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void leashResetsAnAbandonedFight(GameTestHelper helper) {
		FrostBearGameTests.leashResetsAnAbandonedFight(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void bearFrostPiercesHeartOfWinter(GameTestHelper helper) {
		FrostBearGameTests.bearFrostPiercesHeartOfWinter(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void bossShrugsOffSlowness(GameTestHelper helper) {
		FrostBearGameTests.bossShrugsOffSlowness(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void warmthMeltsFrostbite(GameTestHelper helper) {
		FrostBearGameTests.warmthMeltsFrostbite(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void slamShattersOnlySoftBlocks(GameTestHelper helper) {
		FrostBearGameTests.slamShattersOnlySoftBlocks(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.MAX_TICKS)
	public void spikesNeverEntombAnything(GameTestHelper helper) {
		FrostBearGameTests.spikesNeverEntombAnything(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.AI_MAX_TICKS)
	public void aiMaulsATargetInReach(GameTestHelper helper) {
		FrostBearGameTests.aiMaulsATargetInReach(helper);
	}

	@GameTest(structure = PLATFORM, maxTicks = FrostBearGameTests.AI_MAX_TICKS)
	public void aiAnswersATargetOnAPillar(GameTestHelper helper) {
		FrostBearGameTests.aiAnswersATargetOnAPillar(helper);
	}
}
