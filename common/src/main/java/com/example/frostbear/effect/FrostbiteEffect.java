package com.example.frostbear.effect;

import com.example.frostbear.FrostBear;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Frostbite: stacking cold, where amplifier 0–4 means 1–5 stacks.
 *
 * <ul>
 *   <li>Each stack slows movement by 8%.</li>
 *   <li>Frost creeps over the victim in proportion to its stacks (kept below the
 *       vanilla freeze-damage threshold, so the overlay is purely a warning).</li>
 *   <li>At {@link Frostbite#NO_SPRINT_STACKS} or more, players cannot sprint
 *       (enforced client-side by {@code LocalPlayerMixin}).</li>
 *   <li>Reaching {@link Frostbite#MAX_STACKS} Shatters the victim.</li>
 *   <li>Warmth (a campfire, fire, lava nearby, or burning) melts a stack every
 *       {@link Frostbite#WARMTH_INTERVAL} ticks.</li>
 * </ul>
 *
 * Always add stacks through {@link Frostbite#apply}, never with a raw effect instance.
 */
public class FrostbiteEffect extends MobEffect {
	private static final int FROST_TICKS_PER_STACK = 28;

	public FrostbiteEffect() {
		super(MobEffectCategory.HARMFUL, 0x8FD8FF);
		this.addAttributeModifier(Attributes.MOVEMENT_SPEED, FrostBear.id("effect.frostbite"),
				-0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		int stacks = amplifier + 1;
		int frost = Math.min(entity.getTicksRequiredToFreeze() - 1, stacks * FROST_TICKS_PER_STACK);
		if (entity.getTicksFrozen() < frost) {
			entity.setTicksFrozen(frost);
		}
		if (stacks >= Frostbite.NO_SPRINT_STACKS && entity.isSprinting()) {
			entity.setSprinting(false);
		}
		if (entity.tickCount % Frostbite.WARMTH_INTERVAL == 0) {
			Frostbite.warm(level, entity);
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
		return true;
	}
}
