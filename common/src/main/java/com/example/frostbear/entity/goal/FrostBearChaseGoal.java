package com.example.frostbear.entity.goal;

import com.example.frostbear.entity.FrostBearEntity;
import com.example.frostbear.entity.FrostBearMove;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;

/**
 * Hunts the target with vanilla pathing, but instead of a single bite it opens a
 * {@link FrostBearMove#MAUL}: a raised paw (the tell), two raking swipes, then a lunging
 * bite that breaks shields. Each hit only lands if the target is still in front of the
 * bear and within reach, so backing off or circling behind dodges it.
 */
public class FrostBearChaseGoal extends MeleeAttackGoal {
	private static final float SWIPE_DAMAGE = 9.0F;
	private static final double SWIPE_REACH = 4.4;
	private static final double BITE_REACH = 4.8;
	private static final int MAUL_COOLDOWN = 20;

	private final FrostBearEntity bear;
	private int maulReadyAt;

	public FrostBearChaseGoal(FrostBearEntity bear) {
		super(bear, 1.25, true);
		this.bear = bear;
	}

	@Override
	public boolean canContinueToUse() {
		return this.bear.getMove() == FrostBearMove.MAUL || super.canContinueToUse();
	}

	@Override
	public void tick() {
		if (this.bear.getMove() == FrostBearMove.MAUL) {
			this.tickMaul();
			return;
		}
		if (this.bear.isBusy()) {
			return; // another move owns the bear; don't path or bite
		}
		super.tick();
		// Big mobs often fail to find a path in tight spots. With the target in plain sight
		// and close, walk straight at it rather than standing still.
		LivingEntity target = this.bear.getTarget();
		if (target != null && this.bear.getNavigation().isDone() && !this.bear.isWithinMeleeAttackRange(target)
				&& this.bear.distanceToSqr(target) < 12.0 * 12.0 && this.bear.hasLineOfSight(target)) {
			this.bear.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.25);
		}
	}

	@Override
	protected void checkAndPerformAttack(LivingEntity target) {
		if (this.canPerformAttack(target) && this.bear.canStartMove() && this.bear.tickCount >= this.maulReadyAt) {
			this.resetAttackCooldown();
			this.bear.startMove(FrostBearMove.MAUL);
		}
	}

	private void tickMaul() {
		LivingEntity target = this.bear.getTarget();
		int tick = this.bear.getMoveTick();
		if (tick == FrostBearMove.MAUL.duration - 1) {
			this.maulReadyAt = this.bear.tickCount + this.bear.cooldown(MAUL_COOLDOWN);
		}
		if (target == null || !(this.bear.level() instanceof ServerLevel level)) {
			return;
		}
		// track during the tell, commit once the claws come down
		if (tick < FrostBearMove.MAUL_SWIPE_1) {
			this.bear.getLookControl().setLookAt(target, 30.0F, 30.0F);
		}
		if (tick == FrostBearMove.MAUL_SWIPE_1 && this.inReach(target, SWIPE_REACH)) {
			this.bear.swipe(level, target, SWIPE_DAMAGE, true);
		} else if (tick == FrostBearMove.MAUL_SWIPE_2 && this.inReach(target, SWIPE_REACH)) {
			this.bear.swipe(level, target, SWIPE_DAMAGE, false);
		} else if (tick == FrostBearMove.MAUL_BITE - 2) {
			// lunge into the bite
			float yaw = this.bear.yBodyRot * ((float) Math.PI / 180.0F);
			Vec3 motion = this.bear.getDeltaMovement();
			this.bear.setDeltaMovement(motion.x - Math.sin(yaw) * 0.35, motion.y, motion.z + Math.cos(yaw) * 0.35);
		} else if (tick == FrostBearMove.MAUL_BITE && this.inReach(target, BITE_REACH)) {
			this.bear.doHurtTarget(level, target);
		}
	}

	/** Within reach and in front of the bear (not behind or beside it). */
	private boolean inReach(LivingEntity target, double reach) {
		Vec3 to = new Vec3(target.getX() - this.bear.getX(), 0.0, target.getZ() - this.bear.getZ());
		if (to.length() > reach || Math.abs(target.getY() - this.bear.getY()) > 3.0) {
			return false;
		}
		float yaw = this.bear.yBodyRot * ((float) Math.PI / 180.0F);
		Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
		return to.lengthSqr() < 1.0 || forward.dot(to.normalize()) > 0.2;
	}
}
