package com.example.frostbear.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** The condensed remnant used to forge and repair Frostfang. */
public class GlacialCoreItem extends Item {
	public GlacialCoreItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.frostbear.glacial_core.lore").withStyle(ChatFormatting.DARK_AQUA));
		tooltip.accept(Component.translatable("tooltip.frostbear.glacial_core.use").withStyle(ChatFormatting.GRAY));
	}
}