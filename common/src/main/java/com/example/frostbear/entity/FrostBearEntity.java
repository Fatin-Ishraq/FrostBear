package com.example.frostbear.entity;

import java.util.List;
import com.example.frostbear.FrostBear;
import com.example.frostbear.effect.Frostbite;
import com.example.frostbear.entity.goal.FrostBearBreathGoal;
import com.example.frostbear.entity.goal.FrostBearCataclysmGoal;
import com.example.frostbear.entity.goal.FrostBearChargeGoal;
import com.example.frostbear.entity.goal.FrostBearChaseGoal;
import com.example.frostbear.entity.goal.FrostBearEruptionGoal;
import com.example.frostbear.entity.goal.FrostBearIceRingGoal;
import com.example.frostbear.entity.goal.FrostBearLeapGoal;
import com.example.frostbear.entity.goal.FrostBearRoarGoal;
import com.example.frostbear.entity.goal.FrostBearSpikeColumnGoal;
import com.example.frostbear.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The FrostBear boss.
 *
 * <p>The fight has two phases. Phase 1 uses the maul, roar, frost breath, ground slam,
 * and charge. Below half health the bear roars into phase 2 (Heart of Winter): it cracks
 * and glows, moves faster, recovers sooner, and adds the Ice Ring, Frost Leap, and the
 * interruptible Glacial Cataclysm.
 *
 * <p>The rules that keep the fight honest live here:
 * <ul>
 *   <li><b>Frontal guard</b>: hits from in front deal 40% less; sides and back take full damage.
 *       Any fire on the bear thaws the guard for a few seconds.</li>
 *   <li><b>Stun</b>: after crashing into a wall or a broken Cataclysm, it takes double damage.</li>
 *   <li><b>No safe spots</b>: a target it cannot path to for 3 seconds gets a spike column
 *       or a leap, and with mob griefing on, its slams and landings shatter soft blocks.</li>
 *   <li><b>Leash</b>: with no players within range for 15 seconds, it heals and resets.</li>
 *   <li>Health scales with nearby players; Slowness and Weakness last half as long on it.</li>
 * </ul>
 * One move runs at a time ({@link FrostBearMove}); goals start them and drive their effects.
 */
public class FrostBearEntity extends Monster {
	public static final float PHASE_TWO_HEALTH = 0.5F;
	public static final float GUARD_REDUCTION = 0.4F;
	public static final int THAW_TICKS = 100;
	public static final float STUN_DAMAGE_MULTIPLIER = 2.0F;
	public static final int CHARGE_STUN_TICKS = 50;
	public static final int CATACLYSM_STUN_TICKS = 70;
	public static final float CATACLYSM_INTERRUPT_DAMAGE = 30.0F;
	public static final double LEASH_RANGE = 32.0;
	public static final int LEASH_TICKS = 300;
	public static final int UNREACHABLE_TICKS = 60;
	public static final int MAX_SCALED_PLAYERS = 4;
	/** How far (centre to centre, horizontally) the maul's claws reach; it starts a maul inside this. */
	public static final double MAUL_RANGE = 3.6;
	public static final double HEALTH_PER_EXTRA_PLAYER = 0.5;

	/** Blocks the bear may shatter (only while the mob griefing game rule is on). */
	public static final TagKey<Block> BREAKABLE =
			TagKey.create(Registries.BLOCK, FrostBear.id("frost_bear_breakable"));

	private static final double GUARD_ARC_COS = 0.5; // within 60° of straight ahead
	private static final AttributeModifier ENRAGED_SPEED = new AttributeModifier(
			FrostBear.id("enraged_speed"), 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	private static final net.minecraft.resources.Identifier PLAYER_SCALING = FrostBear.id("player_scaling");

	private static final EntityDataAccessor<Integer> MOVE =
			SynchedEntityData.defineId(FrostBearEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> MOVE_TICK =
			SynchedEntityData.defineId(FrostBearEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> MOVE_LENGTH =
			SynchedEntityData.defineId(FrostBearEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> PHASE =
			SynchedEntityData.defineId(FrostBearEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> GUARD_THAWED =
			SynchedEntityData.defineId(FrostBearEntity.class, EntityDataSerializers.BOOLEAN);

	private final ServerBossEvent bossBar =
			new ServerBossEvent(this.getUUID(), this.getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);

	private int thawTicks;
	private int leashTicks;
	private int unreachableTicks;
	private float cataclysmDamage;
	private @Nullable BlockPos home;

	public FrostBearEntity(EntityType<? extends Monster> entityType, Level world) {
		super(entityType, world);
		this.xpReward = 150;
	}

	public static AttributeSupplier.Builder createFrostBearAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 500.0)
				.add(Attributes.ARMOR, 10.0)
				.add(Attributes.ARMOR_TOUGHNESS, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.27)
				.add(Attributes.ATTACK_DAMAGE, 14.0)
				.add(Attributes.ATTACK_KNOCKBACK, 1.4)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
				.add(Attributes.FOLLOW_RANGE, 40.0)
				// tall boss: let it stride up 1.5-block ledges so terrain doesn't stop it
				.add(Attributes.STEP_HEIGHT, 1.5);
	}

	/** Wolf/polar-bear-style spawn rule: solid ground below, on the surface in daylight. */
	public static boolean canNaturallySpawn(EntityType<FrostBearEntity> type,
			net.minecraft.world.level.ServerLevelAccessor world, net.minecraft.world.entity.EntitySpawnReason reason,
			BlockPos pos, net.minecraft.util.RandomSource random) {
		return world.getBlockState(pos.below()).isValidSpawn(world, pos.below(), type)
				&& world.getRawBrightness(pos, 0) > 8;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new FrostBearCataclysmGoal(this));
		FrostBearLeapGoal leap = new FrostBearLeapGoal(this);
		this.goalSelector.addGoal(2, leap);
		this.goalSelector.addGoal(2, new FrostBearSpikeColumnGoal(this, leap));
		this.goalSelector.addGoal(3, new FrostBearIceRingGoal(this));
		this.goalSelector.addGoal(3, new FrostBearRoarGoal(this));
		this.goalSelector.addGoal(4, new FrostBearEruptionGoal(this));
		this.goalSelector.addGoal(5, new FrostBearBreathGoal(this));
		this.goalSelector.addGoal(6, new FrostBearChargeGoal(this));
		this.goalSelector.addGoal(7, new FrostBearChaseGoal(this));
		this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 14.0f));
		this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(MOVE, FrostBearMove.NONE.ordinal());
		builder.define(MOVE_TICK, 0);
		builder.define(MOVE_LENGTH, 0);
		builder.define(PHASE, 1);
		builder.define(GUARD_THAWED, false);
	}

	// ------------------------------------------------------------------------------------
	// Moves
	// ------------------------------------------------------------------------------------

	public FrostBearMove getMove() {
		return FrostBearMove.byId(this.entityData.get(MOVE));
	}

	/** Ticks since the current move started. */
	public int getMoveTick() {
		return this.entityData.get(MOVE_TICK);
	}

	public int getMoveLength() {
		return this.entityData.get(MOVE_LENGTH);
	}

	/** True while any move runs; idle behaviour and other moves wait. */
	public boolean isBusy() {
		return this.getMove() != FrostBearMove.NONE;
	}

	public boolean canStartMove() {
		return !this.isBusy() && this.isAlive();
	}

	public void startMove(FrostBearMove move) {
		this.startMove(move, move.duration);
	}

	public void startMove(FrostBearMove move, int length) {
		this.entityData.set(MOVE, move.ordinal());
		this.entityData.set(MOVE_TICK, 0);
		this.entityData.set(MOVE_LENGTH, length);
	}

	public void endMove() {
		this.startMove(FrostBearMove.NONE, 0);
	}

	private void tickMove() {
		FrostBearMove move = this.getMove();
		if (move == FrostBearMove.NONE) {
			return;
		}
		int tick = this.getMoveTick() + 1;
		this.entityData.set(MOVE_TICK, tick);
		// goals usually end their moves sooner (a hit, a landing); the length is the cap
		if (tick >= this.getMoveLength()) {
			this.endMove();
		}
	}

	// ------------------------------------------------------------------------------------
	// Phases
	// ------------------------------------------------------------------------------------

	public int getPhase() {
		return this.entityData.get(PHASE);
	}

	public boolean isEnraged() {
		return this.getPhase() >= 2;
	}

	/** Cooldowns shrink in phase 2 and on Hard. */
	public int cooldown(int baseTicks) {
		float scale = this.isEnraged() ? 0.7F : 1.0F;
		if (this.level().getDifficulty() == Difficulty.HARD) {
			scale *= 0.85F;
		}
		return Math.round(baseTicks * scale);
	}

	/** Enters phase 2 once health falls to half. */
	public void checkPhase(ServerLevel level) {
		if (!this.isEnraged() && this.isAlive() && this.getHealth() <= this.getMaxHealth() * PHASE_TWO_HEALTH) {
			this.enterPhaseTwo(level);
		}
	}

	private void enterPhaseTwo(ServerLevel level) {
		this.entityData.set(PHASE, 2);
		AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.addOrReplacePermanentModifier(ENRAGED_SPEED);
		}
		this.bossBar.setColor(BossEvent.BossBarColor.PURPLE);
		this.startMove(FrostBearMove.TRANSITION);
		this.thawTicks = 0;
		this.entityData.set(GUARD_THAWED, false);

		// burst of crystal and a push that clears space around it
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()),
				this.getX(), this.getY(1.0), this.getZ(), 80, 1.4, 0.8, 1.4, 0.25);
		level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(0.6), this.getZ(), 60, 2.0, 1.0, 2.0, 0.15);
		this.playSound(ModSounds.FROST_BEAR_ROAR.get(), 3.0F, 0.75F);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
		for (Player player : this.nearbyPlayers(level, 7.0)) {
			Vec3 push = player.position().subtract(this.position()).normalize();
			player.push(push.x * 1.3, 0.45, push.z * 1.3);
			player.hurtMarked = true;
		}
	}

	// ------------------------------------------------------------------------------------
	// Damage rules: transition immunity, stun, frontal guard, fire thaw, Cataclysm interrupt
	// ------------------------------------------------------------------------------------

	public boolean isGuardUp() {
		return this.thawTicks <= 0 && this.getMove() != FrostBearMove.STUNNED;
	}

	public boolean isGuardThawed() {
		return this.entityData.get(GUARD_THAWED);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// The bear never burns, but fire on its ice melts the guard (and breaks a Cataclysm).
		if (source.is(DamageTypeTags.IS_FIRE)) {
			this.thaw(level);
		}
		boolean unstoppable = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
		if (this.getMove() == FrostBearMove.TRANSITION && !unstoppable) {
			this.clink(level, source);
			return false;
		}

		float scaled = amount;
		if (this.getMove() == FrostBearMove.STUNNED) {
			scaled *= STUN_DAMAGE_MULTIPLIER;
		} else if (!unstoppable && this.isGuardUp() && this.isFrontal(source)) {
			scaled *= 1.0F - GUARD_REDUCTION;
			this.clink(level, source);
		}

		boolean hurt = super.hurtServer(level, source, scaled);
		if (hurt && this.getMove() == FrostBearMove.CATACLYSM) {
			this.cataclysmDamage += scaled;
			if (this.cataclysmDamage >= CATACLYSM_INTERRUPT_DAMAGE) {
				this.interruptCataclysm(level);
			}
		}
		if (hurt) {
			this.checkPhase(level);
		}
		return hurt;
	}

	/** True when the source of this damage is within the bear's armoured front arc. */
	public boolean isFrontal(DamageSource source) {
		Vec3 from = source.getSourcePosition();
		if (from == null) {
			return false;
		}
		Vec3 toSource = new Vec3(from.x - this.getX(), 0.0, from.z - this.getZ());
		if (toSource.lengthSqr() < 1.0e-4) {
			return false;
		}
		float yaw = this.yBodyRot * ((float) Math.PI / 180.0F);
		Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
		return forward.dot(toSource.normalize()) > GUARD_ARC_COS;
	}

	/** Feedback for a glancing hit on the guard, so players learn to flank. */
	private void clink(ServerLevel level, DamageSource source) {
		Vec3 at = source.getSourcePosition();
		Vec3 hit = at == null ? this.position().add(0, 1.5, 0)
				: this.position().add(0, 1.5, 0).add(at.subtract(this.position()).normalize().scale(1.3));
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
				hit.x, hit.y, hit.z, 8, 0.25, 0.25, 0.25, 0.1);
		level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.4F, 0.6F);
	}

	/** Melts the frontal guard for a few seconds. Called for any fire touching the bear. */
	public void thaw(ServerLevel level) {
		if (this.thawTicks <= 0) {
			level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(0.6), this.getZ(), 24, 1.0, 0.8, 1.0, 0.04);
			level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.2F, 0.7F);
		}
		this.thawTicks = THAW_TICKS;
		this.entityData.set(GUARD_THAWED, true);
		if (this.getMove() == FrostBearMove.CATACLYSM) {
			this.interruptCataclysm(level);
		}
	}

	@Override
	public void setRemainingFireTicks(int remainingTicks) {
		// Fire Aspect, Flame arrows and the like try to ignite it: thaw instead of burning.
		if (remainingTicks > 0 && this.level() instanceof ServerLevel level) {
			this.thaw(level);
		}
		super.setRemainingFireTicks(0);
	}

	public void stun(ServerLevel level, int ticks) {
		this.startMove(FrostBearMove.STUNNED, ticks);
		this.getNavigation().stop();
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 2.0F, 0.6F);
	}

	public void beginCataclysm() {
		this.cataclysmDamage = 0.0F;
		this.startMove(FrostBearMove.CATACLYSM);
		this.bossBar.setColor(BossEvent.BossBarColor.WHITE);
		this.bossBar.setDarkenScreen(true);
	}

	public void endCataclysm() {
		this.bossBar.setColor(this.isEnraged() ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.BLUE);
		this.bossBar.setDarkenScreen(false);
	}

	private void interruptCataclysm(ServerLevel level) {
		this.endCataclysm();
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()),
				this.getX(), this.getY(1.0), this.getZ(), 60, 1.2, 1.0, 1.2, 0.3);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
		this.stun(level, CATACLYSM_STUN_TICKS);
	}

	/** Slowness and Weakness only last half as long on the boss. */
	@Override
	public boolean addEffect(MobEffectInstance effect, @Nullable Entity source) {
		if (effect.is(MobEffects.SLOWNESS) || effect.is(MobEffects.WEAKNESS)) {
			effect = new MobEffectInstance(effect.getEffect(), Math.max(1, effect.getDuration() / 2),
					effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon());
		}
		return super.addEffect(effect, source);
	}

	// ------------------------------------------------------------------------------------
	// Attacks shared by the goals
	// ------------------------------------------------------------------------------------

	/** The maul's bite: full attack damage, Frostbite, Mining Fatigue, and it breaks shields. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		ItemStack shield = target instanceof LivingEntity living ? living.getItemBlockingWith() : null;
		boolean hit = super.doHurtTarget(level, target);
		if (target instanceof LivingEntity living) {
			if (shield != null) {
				BlocksAttacks blocking = shield.get(DataComponents.BLOCKS_ATTACKS);
				if (blocking != null) {
					blocking.disable(level, living, 5.0F, shield);
				}
			}
			if (hit) {
				living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 1));
				Frostbite.apply(living, 1, this);
			}
		}
		return hit;
	}

	/** A raking claw: lighter than the bite, with a sideways shove. */
	public void swipe(ServerLevel level, LivingEntity target, float damage, boolean fromRight) {
		if (target.hurtServer(level, this.damageSources().mobAttack(this), damage)) {
			float yaw = this.yBodyRot * ((float) Math.PI / 180.0F);
			double side = fromRight ? 1.0 : -1.0;
			target.knockback(0.6, Math.sin(yaw) - Math.cos(yaw) * side * 0.6, -Math.cos(yaw) - Math.sin(yaw) * side * 0.6,
					this.damageSources().mobAttack(this), damage);
		}
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.6F);
	}

	/** Damage, Frostbite and knockback for everyone in a radius (leap landings). */
	public void shockwave(ServerLevel level, Vec3 center, double radius, float damage, int frostbite) {
		List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class,
				this.getBoundingBox().inflate(radius, 1.5, radius), e -> e != this && e.isAlive()
						&& e.position().distanceToSqr(center) <= radius * radius);
		for (LivingEntity target : hit) {
			if (target.hurtServer(level, this.damageSources().mobAttack(this), damage)) {
				Frostbite.apply(target, frostbite, this);
			}
			Vec3 away = target.position().subtract(center).normalize();
			target.push(away.x * 1.1, 0.5, away.z * 1.1);
			target.hurtMarked = true;
		}
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()),
				center.x, center.y + 0.2, center.z, 70, radius * 0.5, 0.2, radius * 0.5, 0.2);
		level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.2, center.z, 24, radius * 0.4, 0.1, radius * 0.4, 0.06);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 0.6F);
	}

	public boolean canGrief(ServerLevel level) {
		return level.getGameRules().get(GameRules.MOB_GRIEFING);
	}

	/**
	 * Shatters soft blocks (snow, ice, dirt, sand, leaves...) in a box, if mob griefing is on.
	 *
	 * @return how many blocks broke
	 */
	public int smashSoftBlocks(ServerLevel level, BlockPos from, BlockPos to, int limit) {
		if (!this.canGrief(level)) {
			return 0;
		}
		int broken = 0;
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			if (broken >= limit) {
				break;
			}
			BlockState state = level.getBlockState(pos);
			if (state.is(BREAKABLE) && !state.is(BlockTags.WITHER_IMMUNE) && level.destroyBlock(pos, true, this)) {
				broken++;
			}
		}
		return broken;
	}

	/** Soft blocks in a ring around the bear, from its feet to above its head. */
	public int smashAround(ServerLevel level, int radius) {
		BlockPos feet = this.blockPosition();
		return this.smashSoftBlocks(level, feet.offset(-radius, 0, -radius), feet.offset(radius, 3, radius), 48);
	}

	public List<Player> nearbyPlayers(ServerLevel level, double range) {
		return level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(range),
				p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(this) <= range * range);
	}

	// ------------------------------------------------------------------------------------
	// Reach, leash, and player scaling
	// ------------------------------------------------------------------------------------

	/** The bear is big: measure melee range from its centre to match the maul's reach. */
	@Override
	public boolean isWithinMeleeAttackRange(LivingEntity target) {
		double horizontal = Math.hypot(target.getX() - this.getX(), target.getZ() - this.getZ());
		return horizontal <= MAUL_RANGE + target.getBbWidth() * 0.5 && Math.abs(target.getY() - this.getY()) < 2.5;
	}

	/** True once the current target has been out of reach for a few seconds. */
	public boolean isTargetUnreachable() {
		return this.unreachableTicks >= UNREACHABLE_TICKS;
	}

	public void clearUnreachable() {
		this.unreachableTicks = 0;
	}

	private void tickReach() {
		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive() || this.isBusy()) {
			if (target == null) {
				this.unreachableTicks = 0;
			}
			return;
		}
		if (this.tickCount % 10 != 0) {
			return;
		}
		double dy = target.getY() - this.getY();
		double horizontal = Math.hypot(target.getX() - this.getX(), target.getZ() - this.getZ());
		boolean perched = dy > 2.5 && horizontal < 6.0; // on a pillar, a ledge, a tower
		boolean blocked = false;
		// a target the claws can reach, or one in plain sight on level ground, is never "unreachable"
		// (big mobs often fail to path in tight spots; the chase goal walks straight at it instead)
		if (!perched && !this.isWithinMeleeAttackRange(target)
				&& !(Math.abs(dy) < 1.5 && horizontal < 8.0 && this.hasLineOfSight(target))) {
			Path path = this.getNavigation().createPath(target, 1);
			blocked = path == null || !path.canReach();
		}
		this.unreachableTicks = perched || blocked ? this.unreachableTicks + 10 : 0;
	}

	/** With no players nearby for a while, heal fully and reset the fight. */
	public void tickLeash(ServerLevel level) {
		if (!this.nearbyPlayers(level, LEASH_RANGE).isEmpty()) {
			this.leashTicks = 0;
			return;
		}
		if (++this.leashTicks >= LEASH_TICKS && (this.getHealth() < this.getMaxHealth() || this.isEnraged())) {
			this.resetFight();
		}
	}

	public void resetFight() {
		this.leashTicks = 0;
		this.unreachableTicks = 0;
		this.thawTicks = 0;
		this.entityData.set(GUARD_THAWED, false);
		this.entityData.set(PHASE, 1);
		this.endMove();
		this.endCataclysm();
		this.setTarget(null);
		AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(ENRAGED_SPEED.id());
		}
		AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.removeModifier(PLAYER_SCALING);
		}
		this.setHealth(this.getMaxHealth());
		this.bossBar.setColor(BossEvent.BossBarColor.BLUE);
	}

	/** Each extra player nearby adds half the base health (up to four players); never shrinks mid-fight. */
	private void tickPlayerScaling(ServerLevel level) {
		if (this.tickCount % 40 != 0) {
			return;
		}
		int players = Math.min(MAX_SCALED_PLAYERS, this.nearbyPlayers(level, LEASH_RANGE).size());
		double bonus = HEALTH_PER_EXTRA_PLAYER * Math.max(0, players - 1);
		AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
		if (health == null) {
			return;
		}
		AttributeModifier current = health.getModifier(PLAYER_SCALING);
		if (bonus <= (current == null ? 0.0 : current.amount())) {
			return;
		}
		float fraction = this.getHealth() / this.getMaxHealth();
		health.addOrReplacePermanentModifier(new AttributeModifier(PLAYER_SCALING, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		this.setHealth(this.getMaxHealth() * fraction);
	}

	private void tickHome() {
		if (this.home == null) {
			this.home = this.blockPosition();
		}
		if (this.getTarget() == null && !this.isBusy() && this.tickCount % 40 == 0
				&& this.home.distSqr(this.blockPosition()) > 16 * 16) {
			this.getNavigation().moveTo(this.home.getX() + 0.5, this.home.getY(), this.home.getZ() + 0.5, 1.0);
		}
	}

	// ------------------------------------------------------------------------------------
	// Tick
	// ------------------------------------------------------------------------------------

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		this.tickMove();
		if (this.thawTicks > 0 && --this.thawTicks == 0) {
			this.entityData.set(GUARD_THAWED, false);
		}
		if (this.thawTicks > 0 && this.tickCount % 6 == 0) {
			level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(0.7), this.getZ(), 2, 0.8, 0.6, 0.8, 0.01);
		}
		if (this.getMove() == FrostBearMove.STUNNED && this.tickCount % 4 == 0) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(1.05), this.getZ(), 3, 0.6, 0.15, 0.6, 0.02);
		}
		this.checkPhase(level);
		this.tickLeash(level);
		this.tickPlayerScaling(level);
		this.tickReach();
		this.tickHome();
		this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
		// hold still while committed to a move so the pose reads clearly (charge and leap drive their own motion)
		if (this.isBusy()) {
			this.getNavigation().stop();
		}
	}

	// ------------------------------------------------------------------------------------
	// Boss bar, saving, misc
	// ------------------------------------------------------------------------------------

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

	/** Keep the boss bar in sync with name tags and custom names. */
	@Override
	public void setCustomName(Component name) {
		super.setCustomName(name);
		this.bossBar.setName(this.getDisplayName());
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Phase", this.getPhase());
		output.storeNullable("Home", BlockPos.CODEC, this.home);
	}

	@Override
	public void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(PHASE, input.getIntOr("Phase", 1));
		this.home = input.read("Home", BlockPos.CODEC).orElse(null);
		this.bossBar.setColor(this.isEnraged() ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.BLUE);
		if (this.hasCustomName()) {
			this.bossBar.setName(this.getDisplayName());
		}
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return false;
	}

	/** Its own leaps would otherwise hurt it. */
	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
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
		return ModSounds.FROST_BEAR_GROWL.get();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.FROST_BEAR_HURT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.FROST_BEAR_DEATH.get();
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(ModSounds.FROST_BEAR_STEP.get(), 0.5f, 1.0f);
	}

	@Override
	protected float getSoundVolume() {
		return 1.4f;
	}
}
