package com.example.frostbear.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.example.frostbear.registry.ModEntities;

/**
 * A block-driven ice spike: a short pillar of ice erupts from the ground
 * (placed bottom-up over a few ticks), launches + freezes anything above it,
 * holds, then shatters into ice particles. The entity itself is invisible — the
 * ice blocks it places are the visual, so it always renders correctly and syncs
 * for free. Spawned in a ripple wave by the Frostfang or the FrostBear.
 */
public class IceSpikeEntity extends Entity {
	/**
	 * Send the change to clients, but skip neighbour block updates and drops — these blocks
	 * live for a second and are gone. Full updates would cascade (water flow, block ticks,
	 * redstone, observers) and dirty chunks far beyond the spike for no visible gain.
	 */
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

	private static final int RISE = 5;
	private static final int HOLD_END = 16;
	private static final int LIFE = 22;

	private int stagger;
	private int ticks;
	private int maxHeight = 2;
	private boolean struck;
	private UUID ownerUuid;
	private final List<BlockPos> placed = new ArrayList<>();

	public IceSpikeEntity(EntityType<? extends IceSpikeEntity> type, Level world) {
		super(type, world);
		this.noPhysics = true;
		this.setNoGravity(true);
		this.setInvisible(true);
	}

	public void setOwner(LivingEntity owner) {
		this.ownerUuid = owner != null ? owner.getUUID() : null;
	}

	public void setMaxHeight(int height) {
		this.maxHeight = Math.max(1, height);
	}

	public void setStagger(int stagger) {
		this.stagger = stagger;
	}

	/** Spawn a ripple wave of spikes stepping outward from an origin toward a direction. */
	public static void spawnWave(Level world, double ox, double oy, double oz, Vec3 dir, int count, LivingEntity owner) {
		Vec3 horiz = new Vec3(dir.x, 0.0, dir.z);
		if (horiz.lengthSqr() < 1.0e-4) {
			return;
		}
		horiz = horiz.normalize();
		for (int i = 1; i <= count; i++) {
			double dist = 1.5 + i * 1.6;
			double x = ox + horiz.x * dist;
			double z = oz + horiz.z * dist;
			double y = findGround(world, x, oy, z);
			IceSpikeEntity spike = new IceSpikeEntity(ModEntities.ICE_SPIKE.get(), world);
			spike.snapTo(x, y, z, 0.0F, 0.0F);
			spike.setOwner(owner);
			spike.setMaxHeight(2 + world.getRandom().nextInt(2));
			spike.setStagger((i - 1) * 2);
			world.addFreshEntity(spike);
		}
	}

	private static double findGround(Level world, double x, double y, double z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(net.minecraft.util.Mth.floor(x),
				net.minecraft.util.Mth.floor(y) + 2, net.minecraft.util.Mth.floor(z));
		for (int i = 0; i < 6; i++) {
			if (!world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir()) {
				return pos.getY() + 1;
			}
			pos.move(0, -1, 0);
		}
		return y;
	}

	@Override
	public void tick() {
		if (this.stagger > 0) {
			this.stagger--;
			return;
		}
		if (this.level().isClientSide()) {
			return; // purely visual state lives in the (synced) blocks
		}

		this.ticks++;
		if (this.ticks <= RISE) {
			int level = (this.ticks - 1) / 2;
			if (level < this.maxHeight) {
				placeLevel(level);
			}
		}
		if (this.ticks == 1 && !this.struck) {
			this.struck = true;
			strike();
		}
		if (this.ticks == HOLD_END) {
			shatter();
		}
		if (this.ticks >= LIFE) {
			this.discard();
		}
	}

	private void placeLevel(int level) {
		BlockPos pos = this.blockPosition().above(level);
		BlockState state = this.level().getBlockState(pos);
		if (state.isAir() || state.canBeReplaced()) {
			BlockState ice = level >= this.maxHeight - 1 ? Blocks.ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
			this.level().setBlock(pos, ice, FLAGS);
			this.placed.add(pos.immutable());
			if (level == 0) {
				this.level().playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.9f, 0.6f);
			}
		}
	}

	private void strike() {
		Entity owner = this.ownerEntity();
		AABB hit = new AABB(this.getX() - 0.7, this.getY() - 0.2, this.getZ() - 0.7,
				this.getX() + 0.7, this.getY() + this.maxHeight + 0.6, this.getZ() + 0.7);
		for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, hit,
				e -> e != owner && e.isAlive())) {
			target.hurt(this.damageSources().freeze(), 6.0f);
			target.push(0.0, 0.55, 0.0);
			target.hurtMarked = true;
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
			target.setTicksFrozen(target.getTicksFrozen() + 120);
		}
		if (this.level() instanceof ServerLevel sw) {
			sw.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 0.6, this.getZ(),
					14, 0.3, 0.5, 0.3, 0.03);
		}
	}

	private void shatter() {
		for (BlockPos pos : this.placed) {
			BlockState state = this.level().getBlockState(pos);
			if (state.is(Blocks.PACKED_ICE) || state.is(Blocks.ICE)) {
				this.level().levelEvent(2001, pos, Block.getId(state)); // block-break FX
				this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
			}
		}
		this.placed.clear();
	}

	private Entity ownerEntity() {
		return (this.ownerUuid != null && this.level() instanceof ServerLevel sw) ? sw.getEntity(this.ownerUuid) : null;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!this.level().isClientSide() && reason == RemovalReason.DISCARDED && !this.placed.isEmpty()) {
			shatter();
		}
		super.remove(reason);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}
	@Override
	public boolean shouldBeSaved() {
		return false; // transient effect — never persist to disk
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
