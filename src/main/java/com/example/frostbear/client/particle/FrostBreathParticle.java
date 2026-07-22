package com.example.frostbear.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** A soft icy puff for the FrostBear's breath: drifting, rising, and fading. */
public class FrostBreathParticle extends SingleQuadParticle {
	private final SpriteSet sprites;

	FrostBreathParticle(ClientLevel level, double x, double y, double z,
			double vx, double vy, double vz, SpriteSet sprites) {
		super(level, x, y, z, sprites.first());
		this.sprites = sprites;
		this.xd = vx;
		this.yd = vy;
		this.zd = vz;
		this.quadSize *= 1.0F + this.random.nextFloat() * 0.9F;
		this.lifetime = 12 + this.random.nextInt(10);
		this.gravity = -0.015F;
		this.hasPhysics = false;
		this.rCol = 0.85F;
		this.gCol = 0.95F;
		this.bCol = 1.0F;
		this.alpha = 0.9F;
		this.setSpriteFromAge(sprites);
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	@Override
	public void tick() {
		super.tick();
		this.setSpriteFromAge(this.sprites);
		this.alpha = 0.9F * (1.0F - (float) this.age / this.lifetime);
	}

	public static class Factory implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Factory(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
				double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
			return new FrostBreathParticle(level, x, y, z, vx, vy, vz, this.sprites);
		}
	}
}