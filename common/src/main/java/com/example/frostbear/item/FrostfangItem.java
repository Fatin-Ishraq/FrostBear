package com.example.frostbear.item;

import java.util.function.Consumer;
import com.example.frostbear.effect.Frostbite;
import com.example.frostbear.entity.IceSpikeEntity;
import com.example.frostbear.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Frost-forged blade: every hit adds Frostbite, hits on already-Frostbitten
 * targets deal +20% damage, and right-click erupts an ice-spike wave.
 */
public class FrostfangItem extends Item {
	private static final int COOLDOWN = 120;
	private static final int SPIKES = 6;
	private static final float FROSTBITTEN_BONUS = 0.2F;

	public FrostfangItem(Properties properties) {
		super(properties);
	}

	/** Bonus damage against targets that already have Frostbite (checked before this hit adds a stack). */
	@Override
	public float getAttackDamageBonus(Entity victim, float damage, DamageSource damageSource) {
		if (victim instanceof LivingEntity living && Frostbite.stacks(living) > 0) {
			return damage * FROSTBITTEN_BONUS;
		}
		return 0.0F;
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		Frostbite.apply(target, 1, attacker);
		super.hurtEnemy(stack, target, attacker);
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);
		if (user.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}

		if (!level.isClientSide()) {
			IceSpikeEntity.spawnWave(level, user.getX(), user.getY(), user.getZ(),
					user.getViewVector(1.0F), SPIKES, user);
			level.playSound(null, user.getX(), user.getY(), user.getZ(),
					ModSounds.FROST_BEAR_CHARGE.get(), SoundSource.PLAYERS, 1.0F, 0.9F);
			level.playSound(null, user.getX(), user.getY(), user.getZ(),
					SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 0.65F);
			if (level instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
						user.getX(), user.getY(0.5), user.getZ(), 28, 0.7, 0.7, 0.7, 0.08);
			}
		}

		user.getCooldowns().addCooldown(stack, COOLDOWN);
		return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.frostbear.frostfang.hit").withStyle(ChatFormatting.AQUA));
		tooltip.accept(Component.translatable("tooltip.frostbear.frostfang.ability").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("tooltip.frostbear.frostfang.cooldown").withStyle(ChatFormatting.DARK_GRAY));
	}
}