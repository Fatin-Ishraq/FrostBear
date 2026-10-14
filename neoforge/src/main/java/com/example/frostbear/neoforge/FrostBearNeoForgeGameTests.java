package com.example.frostbear.neoforge;

import java.util.List;
import java.util.function.Consumer;
import com.example.frostbear.FrostBear;
import com.example.frostbear.gametest.FrostBearGameTests;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the shared {@link FrostBearGameTests} with NeoForge. Only called when
 * {@code GameTestHooks.isGametestEnabled()}, so production games never see them.
 */
final class FrostBearNeoForgeGameTests {
	private record Test(String name, Consumer<GameTestHelper> function, int maxTicks) {
		Test(String name, Consumer<GameTestHelper> function) {
			this(name, function, FrostBearGameTests.MAX_TICKS);
		}
	}

	private static final List<Test> TESTS = List.of(
			new Test("frostbite_stacks_then_shatters", FrostBearGameTests::frostbiteStacksThenShatters),
			new Test("thaw_blocks_frostbite", FrostBearGameTests::thawBlocksFrostbite),
			new Test("frostbite_slows_movement", FrostBearGameTests::frostbiteSlowsMovement),
			new Test("frost_immune_targets_ignore_frostbite", FrostBearGameTests::frostImmuneTargetsIgnoreFrostbite),
			new Test("frostfang_bonus_needs_frostbite", FrostBearGameTests::frostfangBonusNeedsFrostbite),
			new Test("frontal_guard_reduces_front_damage", FrostBearGameTests::frontalGuardReducesFrontDamage),
			new Test("fire_thaws_the_guard", FrostBearGameTests::fireThawsTheGuard),
			new Test("stun_doubles_damage", FrostBearGameTests::stunDoublesDamage),
			new Test("phase_two_begins_at_half_health", FrostBearGameTests::phaseTwoBeginsAtHalfHealth),
			new Test("leash_resets_an_abandoned_fight", FrostBearGameTests::leashResetsAnAbandonedFight),
			new Test("bear_frost_pierces_heart_of_winter", FrostBearGameTests::bearFrostPiercesHeartOfWinter),
			new Test("boss_shrugs_off_slowness", FrostBearGameTests::bossShrugsOffSlowness),
			new Test("warmth_melts_frostbite", FrostBearGameTests::warmthMeltsFrostbite),
			new Test("slam_shatters_only_soft_blocks", FrostBearGameTests::slamShattersOnlySoftBlocks),
			new Test("spikes_never_entomb_anything", FrostBearGameTests::spikesNeverEntombAnything),
			new Test("ai_mauls_a_target_in_reach", FrostBearGameTests::aiMaulsATargetInReach, FrostBearGameTests.AI_MAX_TICKS),
			new Test("ai_answers_a_target_on_a_pillar", FrostBearGameTests::aiAnswersATargetOnAPillar, FrostBearGameTests.AI_MAX_TICKS));

	private FrostBearNeoForgeGameTests() {}

	static void register(IEventBus modBus) {
		DeferredRegister<Consumer<GameTestHelper>> functions =
				DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, FrostBear.MOD_ID);
		for (Test test : TESTS) {
			functions.register(test.name(), test::function);
		}
		functions.register(modBus);

		modBus.addListener(RegisterGameTestsEvent.class, event -> {
			Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(FrostBear.id("default"));
			for (Test test : TESTS) {
				event.registerTest(FrostBear.id(test.name()), new FunctionGameTestInstance(
						ResourceKey.create(Registries.TEST_FUNCTION, FrostBear.id(test.name())),
						new TestData<>(environment, FrostBearGameTests.PLATFORM, test.maxTicks(), 0, true)));
			}
		});
	}
}
