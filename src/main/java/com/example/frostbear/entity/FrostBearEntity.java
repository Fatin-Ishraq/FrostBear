package com.example.frostbear.entity;

import com.example.frostbear.entity.goal.FrostBearBreathGoal;
import com.example.frostbear.entity.goal.FrostBearChargeGoal;
import com.example.frostbear.entity.goal.FrostBearEruptionGoal;
import com.example.frostbear.entity.goal.FrostBearRoarGoal;
import com.example.frostbear.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The FrostBear boss: a massive, ice-hardened polar bear.
 *
 * <ul>
 *   <li>High health with a boss bar shown to nearby players.</li>
 *   <li>Roars periodically ({@link FrostBearRoarGoal}) — an AoE frost blast that
 *       slows and chills nearby players, with a rear-up animation.</li>
 *   <li>Charges targets from range ({@link FrostBearChargeGoal}).</li>
 *   <li>Every melee hit chills the victim (Slowness + freeze).</li>
 *   <li>Immune to fire and freezing; heavily knockback-resistant.</li>
 *   <li>Custom roar / growl / hurt / death / step sounds.</li>
 * </ul>
 */
public class FrostBearEntity extends Monster {
	/** How long a roar animation / timer lasts, in ticks. */
	public static final int ROAR_DURATION = 50;
	/** How long a frost-breath animation / timer lasts, in ticks. */
	public static final int BREATH_DURATION = 44;
	/** How long a ground-slam (ice eruption) animation / timer lasts, in ticks. */
	public static final int SLAM_DURATION = 26;
	/** How long the lowered-head charge pose lasts, in ticks. */
	public static final int CHARGE_DURATION = 30;

	private static final EntityDataAccessor<Integer> ROAR_TICKS =
			SynchedEntityData.defineId(FrostBearEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> BREATH_TICKS =
			SynchedEntityData.defineId(FrostBearEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SLAM_TICKS =
			SynchedEntityData.defineId(FrostBearEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CHARGE_TICKS =
			SynchedEntityData.defineId(FrostBearEntity.class, EntityDataSerializers.INT);

	private final ServerBossEvent bossBar =
			new ServerBossEvent(this.getUUID(), this.getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

	public FrostBearEntity(EntityType<? extends Monster> entityType, Level world) {
		super(entityType, world);
		this.xpReward = 100;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ROAR_TICKS, 0);
		builder.define(BREATH_TICKS, 0);
		builder.define(SLAM_TICKS, 0);
		builder.define(CHARGE_TICKS, 0);
	}

	public int getRoarTicks() {
		return this.entityData.get(ROAR_TICKS);
	}

	public void setRoarTicks(int ticks) {
		this.entityData.set(ROAR_TICKS, ticks);
	}

	public int getBreathTicks() {
		return this.entityData.get(BREATH_TICKS);
	}

	public void setBreathTicks(int ticks) {
		this.entityData.set(BREATH_TICKS, ticks);
	}

	public int getSlamTicks() {
		return this.entityData.get(SLAM_TICKS);
	}

	public void setSlamTicks(int ticks) {
		this.entityData.set(SLAM_TICKS, ticks);
	}

	public int getChargeTicks() {
		return this.entityData.get(CHARGE_TICKS);
	}

	public void setChargeTicks(int ticks) {
		this.entityData.set(CHARGE_TICKS, ticks);
	}

	/** True while the bear is committed to a special move (roar, breath, or slam). */
	public boolean isBusy() {
		return this.getRoarTicks() > 0 || this.getBreathTicks() > 0
				|| this.getSlamTicks() > 0 || this.getChargeTicks() > 0;
	}

	/** Begins a roar (server side): starts the timer and plays the roar sound. */
	public void triggerRoar() {
		if (!this.isBusy()) {
			this.setRoarTicks(ROAR_DURATION);
			this.playSound(ModSounds.FROST_BEAR_ROAR, 2.4F, 1.0F);
		}
	}

	/** Begins a frost-breath exhale (server side). */
	public void triggerBreath() {
		if (!this.isBusy()) {
			this.setBreathTicks(BREATH_DURATION);
			this.playSound(ModSounds.FROST_BEAR_BREATH, 2.0F, 1.0F);
		}
	}

	/** Begins a ground-slam that erupts ice spikes (server side). */
	public void triggerSlam() {
		if (!this.isBusy()) {
			this.setSlamTicks(SLAM_DURATION);
			this.playSound(ModSounds.FROST_BEAR_ROAR, 2.2F, 0.7F);
		}
	}

	/** Begins the charge pose and sound; movement is driven by the charge goal. */
	public void triggerCharge() {
		if (!this.isBusy()) {
			this.setChargeTicks(CHARGE_DURATION);
			this.playSound(ModSounds.FROST_BEAR_CHARGE, 2.2F, 1.0F);
		}
	}

	/** Wolf/polar-bear-style spawn rule: solid ground below, on the surface in daylight. */
	public static boolean canNaturallySpawn(net.minecraft.world.entity.EntityType<FrostBearEntity> type,
			net.minecraft.world.level.ServerLevelAccessor world, net.minecraft.world.entity.EntitySpawnReason reason,
			net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
		return world.getBlockState(pos.below()).isValidSpawn(world, pos.below(), type)
				&& world.getRawBrightness(pos, 0) > 8;
	}

	public static AttributeSupplier.Builder createFrostBearAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 260.0)
				.add(Attributes.MOVEMENT_SPEED, 0.27)
				.add(Attributes.ATTACK_DAMAGE, 16.0)
				.add(Attributes.ATTACK_KNOCKBACK, 1.8)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
				.add(Attributes.FOLLOW_RANGE, 40.0)
				// tall boss: let it stride up 1.5-block ledges so terrain doesn't stop it
				.add(Attributes.STEP_HEIGHT, 1.5);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new FrostBearRoarGoal(this));
		this.goalSelector.addGoal(2, new FrostBearEruptionGoal(this));
		this.goalSelector.addGoal(3, new FrostBearBreathGoal(this));
		this.goalSelector.addGoal(4, new FrostBearChargeGoal(this));
		this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.25, true));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 14.0f));
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** Chill anything the bear bites: Slowness, Mining Fatigue, and a frost buildup. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
			living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 1));
			target.setTicksFrozen(target.getTicksFrozen() + 160);
		}
		return hit;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
		int rt = this.getRoarTicks();
		if (rt > 0) {
			this.setRoarTicks(rt - 1);
		}
		int bt = this.getBreathTicks();
		if (bt > 0) {
			this.setBreathTicks(bt - 1);
		}
		int st = this.getSlamTicks();
		if (st > 0) {
			this.setSlamTicks(st - 1);
		}
		int ct = this.getChargeTicks();
		if (ct > 0) {
			this.setChargeTicks(ct - 1);
		}
		// hold still while committed to a special move so the pose reads clearly
		if (this.isBusy()) {
			this.getNavigation().stop();
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossBar.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossBar.removePlayer(player);
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return false;
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean canFreeze() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.FROST_BEAR_GROWL;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.FROST_BEAR_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.FROST_BEAR_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(ModSounds.FROST_BEAR_STEP, 0.5f, 1.0f);
	}

	@Override
	protected float getSoundVolume() {
		return 1.4f;
	}
}
