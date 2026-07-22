package com.example.frostbear.mixin.client;

import com.example.frostbear.client.FrozenHeartClientEffects;
import com.example.frostbear.item.FrozenHeartItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {

	@Inject(method = "handleEntityEvent", at = @At("HEAD"))
	private void frostbear$showFrozenHeartActivation(ClientboundEntityEventPacket packet, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (packet.getEventId() != FrozenHeartItem.ACTIVATION_STATUS || client.level == null) {
			return;
		}

		Entity entity = packet.getEntity(client.level);
		if (entity instanceof LivingEntity living) {
			FrozenHeartClientEffects.play(client, living);
		}
	}
}