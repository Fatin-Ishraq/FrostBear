package com.example.frostbear.item;

import java.util.function.Consumer;
import com.example.frostbear.registry.ModEffects;
import com.example.frostbear.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** FrostBear trophy heart: activates its powers instantly on right-click. */
public class FrozenHeartItem extends Item {
	private static final int EFFECT_DURATION = 2400;
	public static final byte ACTIVATION_STATUS = 67;

	public FrozenHeartItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);

		if (!level.isClientSide()) {
			user.addEffect(new MobEffectInstance(ModEffects.WINTERS_HEART.holder(), EFFECT_DURATION, 0, false, true, true));
			user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
			level.broadcastEntityEvent(user, ACTIVATION_STATUS);
			level.playSound(null, user.getX(), user.getY(), user.getZ(),
					ModSounds.FROST_BEAR_BREATH.get(), SoundSource.PLAYERS, 1.35F, 0.72F);
			level.playSound(null, user.getX(), user.getY(), user.getZ(),
					SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.9F, 0.65F);
			level.playSound(null, user.getX(), user.getY(), user.getZ(),
					SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4F, 0.8F);
			level.playSound(null, user.getX(), user.getY(), user.getZ(),
					SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.55F, 1.35F);
			if (!user.getAbilities().instabuild) {
				stack.shrink(1);
			}
		}

		user.getCooldowns().addCooldown(stack, 40);
		return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.frostbear.frost_heart.duration").withStyle(ChatFormatting.LIGHT_PURPLE));
		tooltip.accept(Component.translatable("tooltip.frostbear.frost_heart.aura").withStyle(ChatFormatting.AQUA));
		tooltip.accept(Component.translatable("tooltip.frostbear.frost_heart.regeneration").withStyle(ChatFormatting.GRAY));
	}
}