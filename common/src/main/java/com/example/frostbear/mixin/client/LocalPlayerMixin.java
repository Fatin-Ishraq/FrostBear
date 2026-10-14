package com.example.frostbear.mixin.client;

import com.example.frostbear.effect.Frostbite;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sprinting is decided on the client, so heavy Frostbite has to stop it here:
 * vanilla uses this check both to start a sprint and to end one.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

	@Inject(method = "isSprintingPossible", at = @At("RETURN"), cancellable = true)
	private void frostbear$noSprintWhenFrostbitten(boolean allowedInShallowWater, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && Frostbite.stacks((LocalPlayer) (Object) this) >= Frostbite.NO_SPRINT_STACKS) {
			cir.setReturnValue(false);
		}
	}
}
