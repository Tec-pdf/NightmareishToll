package org.belltaffmod.bellstaff_tplayers;

public final class BellstaffConstants {

    private BellstaffConstants() {}

    public static final int PHYSICAL_DAMAGE = 12;
    public static final float MAGIC_DAMAGE = 4.0F;
    public static final float ATTACK_SPEED_MODIFIER = -2.4F;
    public static final int DURABILITY = 1561;
    public static final int ENCHANTABILITY = 22;
    public static final float MINING_SPEED = 2.0F;
    public static final int HARVEST_LEVEL = 2;

    public static final float HIT_SOUND_VOLUME = 1.0F;
    public static final float HIT_SOUND_PITCH = 1.0F;

    public static final boolean AFFECT_ONLY_PLAYERS = false;
    public static final int EFFECT_DURATION_SECONDS = 30;

    public static final double NON_OWNER_HEALTH_STEP = 1.0D;
    public static final int NON_OWNER_LOSS_INTERVAL_TICKS = 20;
    public static final int NON_OWNER_RECOVERY_INTERVAL_TICKS = 15;
    public static final double NON_OWNER_MIN_MAX_HEALTH = 2.0D;
    public static final boolean PENALIZE_CREATIVE_PLAYERS = false;

    public static final boolean PENALIZE_ENTITIES = true;

    public static final float[] TIER_CHANCES = { 0.40F, 0.30F, 0.25F };
    public static final int[] WEAKNESS_AMPLIFIERS = { 0, 1, 3 };
    public static final int[] SLOWNESS_AMPLIFIERS = { 0, 1, 3 };
    public static final float[] FOV_REDUCTIONS = { 10.0F, 10.0F, 20.0F };
    public static final int MAX_TIER = TIER_CHANCES.length;
    public static final float MIN_FOV = 10.0F;

    public static final float CRIT_BONUS_TIER_CHANCE = 0.05F;
    public static final float CRIT_BONUS_MINIGAME_CHANCE = 0.05F;

    public static final float MINIGAME_CHANCE = 0.35F;
    public static final float MINIGAME_FOV = 30.0F;
    public static final int MINIGAME_REQUIRED_PRESSES = 40;
    public static final float MINIGAME_DECAY_PRESSES_PER_SECOND = 2.0F;
    public static final int MINIGAME_TIMEOUT_SECONDS = 60;
    public static final float MINIGAME_MAX_PRESSES_PER_SECOND = 25.0F;
    public static final boolean CLEAR_TIER_EFFECTS_ON_FREEDOM = true;

    public static final int FRIGHT_DURATION_SECONDS = 20;
    public static final double FRIGHT_REPEL_RADIUS = 6.0D;
    public static final double FRIGHT_PUSH_STRENGTH = 0.45D;

    public static final String MALUM_MOD_ID = "malum";
}