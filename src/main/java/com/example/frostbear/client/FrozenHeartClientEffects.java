package com.example.frostbear.client;

import com.example.frostbear.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Client-side presentation for consuming a Frozen Heart. */
public final class FrozenHeartClientEffects {
	private FrozenHeartClientEffects() {
	}

	public static void play(Minecraft client, LivingEntity source) {
		if (client.level == null) {
			return;
		}

		RandomSource random = source.getRandom();
		double centerY = source.getY() + source.getBbHeight() * 0.5;

		// Expanding snow ring: the broad, readable first beat of the activation.
		for (int i = 0; i < 64; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double radius = 0.25 + random.nextDouble() * 1.05;
			double speed = 0.055 + random.nextDouble() * 0.09;
			client.level.addParticle(
					ParticleTypes.SNOWFLAKE,
					source.getX() + Math.cos(angle) * radius,
					source.getY() + 0.15 + random.nextDouble() * source.getBbHeight(),
					source.getZ() + Math.sin(angle) * radius,
					Math.cos(angle) * speed,
					0.045 + random.nextDouble() * 0.12,
					Math.sin(angle) * speed);
		}

		// Sharp cyan sparks make the heart feel like it cracks open rather than
		// merely applying another status effect.
		for (int i = 0; i < 28; i++) {
			client.level.addParticle(
					ParticleTypes.ELECTRIC_SPARK,
					source.getRandomX(1.25),
					source.getRandomY(),
					source.getRandomZ(1.25),
					random.nextGaussian() * 0.055,
					0.08 + random.nextDouble() * 0.12,
					random.nextGaussian() * 0.055);
		}

		// A smaller lingering core gives the burst a bright center.
		for (int i = 0; i < 18; i++) {
			client.level.addParticle(
					ParticleTypes.END_ROD,
					source.getRandomX(0.7),
					centerY + random.nextGaussian() * 0.28,
					source.getRandomZ(0.7),
					random.nextGaussian() * 0.025,
					0.025 + random.nextDouble() * 0.045,
					random.nextGaussian() * 0.025);
		}

		if (source == client.player) {
			client.gameRenderer.displayItemActivation(new ItemStack(ModItems.FROST_HEART));
			client.gui.hud.setOverlayMessage(
					Component.translatable("message.frostbear.frozen_heart_awakened")
							.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
					false);
		}
	}
}