package com.example.frostbear.mixin;

import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.registry.ModEntities;
import com.example.frostbear.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turns a vanilla polar bear into a FrostBear when it is simultaneously
 * afflicted with Slowness AND Weakness (splash both potions on it). Injected on
 * MobEntity.mobTick and filtered to polar bears so it works regardless of
 * whether PolarBearEntity overrides mobTick.
 */
@Mixin(Mob.class)
public class MobEntityMixin {

	@Inject(method = "customServerAiStep", at = @At("TAIL"))
	private void frostbear$convertPolarBear(ServerLevel level, CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		if (self.level().isClientSide() || !(self instanceof PolarBear)) {
			return;
		}
		if (self.hasEffect(MobEffects.SLOWNESS) && self.hasEffect(MobEffects.WEAKNESS)) {
			FrostBearEntity frost = self.convertTo(ModEntities.FROST_BEAR, ConversionParams.single(self, false, false),
					EntitySpawnReason.CONVERSION, converted -> {});
			if (frost != null) {
				self.level().playSound(null, frost.getX(), frost.getY(), frost.getZ(),
						ModSounds.FROST_BEAR_ROAR, SoundSource.HOSTILE, 2.0f, 1.0f);
			}
		}
	}
}
