package com.example.fortifications.spellbalance;

import java.util.Map;

public final class SpellBalanceConfig {

    public static final double ROOT_POWER_MULTIPLIER = 0.50D;

    public static final int BLOOD_STEP_TRUE_INVISIBILITY_TICKS = 2 * 20;
    public static final int OAKSKIN_DURATION_TICKS = 10 * 20;
    public static final int HASTENED_DURATION_TICKS = 20 * 20;
    public static final int PORTAL_DURATION_TICKS = 15 * 20;
    public static final int BLACK_HOLE_DURATION_TICKS = 15 * 20;

    public static final Map<String, int[]> MANA_COSTS = Map.ofEntries(
            Map.entry(FortificationsSpellBalance.GTBC_SPECTRAL_BLINK, new int[] {30, 90, 150}),
            Map.entry(FortificationsSpellBalance.IRON_OAKSKIN, new int[] {45, 60, 75, 90, 105, 120, 135, 150}),
            Map.entry(FortificationsSpellBalance.IRON_CHARGE, new int[] {150, 200, 250}),
            Map.entry(FortificationsSpellBalance.IRON_HASTE, new int[] {150, 200, 250, 300}),
            Map.entry(FortificationsSpellBalance.IRON_TELEKINESIS, new int[] {150, 150, 150, 150, 150}),
            Map.entry(FortificationsSpellBalance.GEOMANCY_PETRIVISE, new int[] {120, 180, 240})
    );

    public static final Map<String, double[]> COOLDOWN_SECONDS_BY_LEVEL = Map.ofEntries(
            Map.entry(FortificationsSpellBalance.IRON_BLOOD_STEP, new double[] {6, 6, 6, 6, 6}),
            Map.entry(FortificationsSpellBalance.IRON_FROST_STEP, new double[] {6, 6, 6, 6, 6, 6, 6, 6}),
            // Raise Hell: increase cooldown by 50% from 40s to 60s.
            Map.entry(FortificationsSpellBalance.IRON_RAISE_HELL, new double[] {60}),
            Map.entry(FortificationsSpellBalance.IRON_FIREBALL, new double[] {20, 18, 16, 13, 10}),
            Map.entry(FortificationsSpellBalance.IRON_LIGHTNING_BOLT, new double[] {20, 18, 16, 15, 14, 12, 10, 8, 6, 5}),
            Map.entry(FortificationsSpellBalance.IRON_VOLT_STRIKE, new double[] {6, 6, 6, 6, 6, 6, 6, 6, 6, 6}),
            Map.entry(FortificationsSpellBalance.IRON_BURNING_DASH, new double[] {6, 6, 6, 6, 6, 6, 6, 6, 6, 6}),
            // Petrivise: double cooldown from 30s to 60s.
            Map.entry(FortificationsSpellBalance.GEOMANCY_PETRIVISE, new double[] {60, 60, 60}),
            Map.entry(FortificationsSpellBalance.GTBC_FLAMES_REBORN, new double[] {180, 180, 180})
    );

    public static final Map<String, Double> POWER_MULTIPLIERS = Map.ofEntries(
            // Root duration is derived from spell power.
            Map.entry(FortificationsSpellBalance.IRON_ROOT, ROOT_POWER_MULTIPLIER)
    );

    public static final Map<String, Integer> CAST_TIME_TICKS = Map.ofEntries(
            Map.entry(FortificationsSpellBalance.IRON_ROOT, 80),
            Map.entry(FortificationsSpellBalance.IRON_FIREBALL, 30),
            // Lightning Lance: 2 seconds to 1 second.
            Map.entry(FortificationsSpellBalance.IRON_LIGHTNING_LANCE, 20),
            Map.entry(FortificationsSpellBalance.GTBC_FLAMES_REBORN, 20)
    );

    public static final Map<String, SpellFields> SPELL_FIELDS = Map.ofEntries(
            Map.entry(FortificationsSpellBalance.IRON_BLOOD_STEP, new SpellFields(null, null, 2, 1)),
            Map.entry(FortificationsSpellBalance.GTBC_SPECTRAL_BLINK, new SpellFields(30, 60, 0, 1)),
            Map.entry(FortificationsSpellBalance.IRON_TELEPORT, new SpellFields(null, null, 2, 1)),
            Map.entry(FortificationsSpellBalance.IRON_PORTAL, new SpellFields(null, null, 15, 15)),
            Map.entry(FortificationsSpellBalance.IRON_OAKSKIN, new SpellFields(45, 15, null, null)),
            Map.entry(FortificationsSpellBalance.IRON_CHARGE, new SpellFields(150, 50, 15, 5)),
            Map.entry(FortificationsSpellBalance.IRON_ROOT, new SpellFields(null, null, 5, 1)),
            Map.entry(FortificationsSpellBalance.IRON_HASTE, new SpellFields(150, 50, 20, 0)),
            Map.entry(FortificationsSpellBalance.IRON_TELEKINESIS, new SpellFields(150, 0, null, null)),
            Map.entry(FortificationsSpellBalance.GEOMANCY_PETRIVISE, new SpellFields(120, 60, null, null))
    );

    public record SpellFields(Integer baseManaCost, Integer manaCostPerLevel, Integer baseSpellPower, Integer spellPowerPerLevel) {}

    private SpellBalanceConfig() {}
}
