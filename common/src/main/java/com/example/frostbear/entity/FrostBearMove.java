package com.example.frostbear.entity;

/**
 * Everything the FrostBear can be committed to. Exactly one move runs at a time; the
 * server ticks it and the client animates from the same move and tick, so timing
 * constants here are shared by the attack logic and the model.
 *
 * <p>Every attack opens with a readable tell of at least 15 ticks (¾ s) before it can
 * hurt anyone, and most leave a recovery window afterwards.
 */
public enum FrostBearMove {
	NONE(0),
	/** Swipe, swipe, bite. The bite disables shields. */
	MAUL(36),
	ROAR(50),
	/** Inhale, then a sweeping cone of frost. */
	BREATH(52),
	/** Rear up and slam: a line of ice spikes, and soft blocks around it shatter. */
	SLAM(30),
	/** Scrape the ground and lower the head; the run-up direction locks at the end. */
	CHARGE_WINDUP(20),
	CHARGE(24),
	/** Head stuck in a wall, or a broken Cataclysm: no guard and double damage taken. */
	STUNNED(50),
	/** Phase 2: hunch, then rings of spikes burst around the bear. */
	ICE_RING(30),
	/** Phase 2: crouch while the landing spot is marked, then leap. */
	LEAP_CROUCH(20),
	/** Airborne until landing (capped), then a shockwave. */
	LEAP(44),
	/** Phase 2: channel, then frost hits everyone not behind cover. Interruptible. */
	CATACLYSM(75),
	/** The roar into phase 2. The bear cannot be hurt while it plays. */
	TRANSITION(50);

	// key frames, in ticks since the move started
	public static final int MAUL_SWIPE_1 = 15;
	public static final int MAUL_SWIPE_2 = 23;
	public static final int MAUL_BITE = 31;
	public static final int ROAR_BLAST = 20;
	public static final int BREATH_EXHALE = 16;
	public static final int BREATH_END = 48;
	public static final int SLAM_IMPACT = 16;
	public static final int RING_BURST = 18;
	public static final int LEAP_LAND_RECOVERY = 12;
	public static final int CATACLYSM_RELEASE = 60;

	public final int duration;

	FrostBearMove(int duration) {
		this.duration = duration;
	}

	public static FrostBearMove byId(int id) {
		FrostBearMove[] values = values();
		return id >= 0 && id < values.length ? values[id] : NONE;
	}
}
