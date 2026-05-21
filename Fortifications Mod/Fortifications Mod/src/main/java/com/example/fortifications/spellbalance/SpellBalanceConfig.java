package com.example.fortifications.spellbalance;

import java.util.Map;

public final class SpellBalanceConfig {

    public static final double RANGE_POWER_MULTIPLIER = 0.10D;
    public static final double SPECTRAL_BLINK_RANGE_POWER_MULTIPLIER = 0.50D;
    public static final double ROOT_POWER_MULTIPLIER = 0.50D;

    public static final float NULLFLARE_DAMAGE_MULTIPLIER = 0.75F;
    public static final float BLACK_HOLE_RADIUS_MULTIPLIER = 0.25F;

    public static final int BLOOD_STEP_TRUE_INVISIBILITY_TICKS = 50;
    public static final int OAKSKIN_DURATION_TICKS = 10 * 20;
    public static final double CHARGE_DURATION_MULTIPLIER = 0.50D;
    public static final double HASTENED_DURATION_MULTIPLIER = 0.75D;
    public static final int PORTAL_DURATION_TICKS = 240;

    public static final Map<String, Double> COOLDOWN_SECONDS = Map.ofEntries(
            // Blood Step: halve cooldown from 12s to 6s.
            Map.entry(FortificationsSpellBalance.IRON_BLOOD_STEP, 6.0D),
            // Frost Step: halve cooldown from 12s to 6s.
            Map.entry(FortificationsSpellBalance.IRON_FROST_STEP, 6.0D),
            // Raise Hell: increase cooldown by 50% from 40s to 60s.
            Map.entry(FortificationsSpellBalance.IRON_RAISE_HELL, 60.0D),
            // Fireball and lightning projectile buffs.
            Map.entry(FortificationsSpellBalance.IRON_FIREBALL, 15.0D),
            Map.entry(FortificationsSpellBalance.IRON_LIGHTNING_BOLT, 15.0D),
            Map.entry(FortificationsSpellBalance.IRON_VOLT_STRIKE, 5.0D),
            Map.entry(FortificationsSpellBalance.IRON_BURNING_DASH, 5.0D),
            // Petrivise: double cooldown from 30s to 60s.
            Map.entry(FortificationsSpellBalance.GEOMANCY_PETRIVISE, 60.0D),
            // Flames Reborn: 2 minutes to 3 minutes.
            Map.entry(FortificationsSpellBalance.GTBC_FLAMES_REBORN, 180.0D)
    );

    public static final Map<String, Double> MANA_MULTIPLIERS = Map.ofEntries(
            // Charge, Hastened, Telekinesis, and Petrivise all cost triple mana.
            Map.entry(FortificationsSpellBalance.IRON_CHARGE, 3.0D),
            Map.entry(FortificationsSpellBalance.IRON_HASTE, 3.0D),
            Map.entry(FortificationsSpellBalance.IRON_TELEKINESIS, 3.0D),
            Map.entry(FortificationsSpellBalance.GEOMANCY_PETRIVISE, 3.0D)
    );

    public static final Map<String, Double> POWER_MULTIPLIERS = Map.ofEntries(
            // These spells calculate range from spell power, so lower power is the reliable server-side range hook.
            Map.entry(FortificationsSpellBalance.IRON_BLOOD_STEP, RANGE_POWER_MULTIPLIER),
            Map.entry(FortificationsSpellBalance.GTBC_SPECTRAL_BLINK, SPECTRAL_BLINK_RANGE_POWER_MULTIPLIER),
            Map.entry(FortificationsSpellBalance.IRON_TELEPORT, RANGE_POWER_MULTIPLIER),
            Map.entry(FortificationsSpellBalance.IRON_FROST_STEP, RANGE_POWER_MULTIPLIER),
            // Root duration is derived from spell power.
            Map.entry(FortificationsSpellBalance.IRON_ROOT, ROOT_POWER_MULTIPLIER)
    );

    public static final Map<String, Integer> CAST_TIME_TICKS = Map.ofEntries(
            // Root: 40 ticks to 100 ticks.
            Map.entry(FortificationsSpellBalance.IRON_ROOT, 100),
            // Lightning Lance: 2 seconds to 1 second.
            Map.entry(FortificationsSpellBalance.IRON_LIGHTNING_LANCE, 20)
    );

    private SpellBalanceConfig() {}
}
