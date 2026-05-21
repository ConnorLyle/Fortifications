package com.example.fortifications.spellbalance;

import io.redspace.ironsspellbooks.api.config.IronConfigParameters;
import io.redspace.ironsspellbooks.api.config.ModifyDefaultConfigValuesEvent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;

import java.lang.reflect.Field;

public final class FortificationsSpellBalance {

    public static final String IRON_ANGEL_WING = "irons_spellbooks:angel_wing";
    public static final String IRON_TOUCH_DIG = "irons_spellbooks:touch_dig";
    public static final String IRON_POCKET_DIMENSION = "irons_spellbooks:pocket_dimension";
    public static final String TUNES_SWIFT_MELODY = "tunes_n_tomes:swift_melody";
    public static final String GTBC_ENSNARE = "gametechbcs_spellbooks:ensnare";
    public static final String GEOMANCY_SEISMIC_SURF = "gtbcs_geomancy_plus:seismic_surf";

    public static final String IRON_BLOOD_STEP = "irons_spellbooks:blood_step";
    public static final String GTBC_SPECTRAL_BLINK = "gametechbcs_spellbooks:spectral_blink";
    public static final String IRON_TELEPORT = "irons_spellbooks:teleport";
    public static final String IRON_PORTAL = "irons_spellbooks:portal";
    public static final String IRON_OAKSKIN = "irons_spellbooks:oakskin";
    public static final String IRON_CHARGE = "irons_spellbooks:charge";
    public static final String IRON_ROOT = "irons_spellbooks:root";
    public static final String IRON_HASTE = "irons_spellbooks:haste";
    public static final String IRON_FROST_STEP = "irons_spellbooks:frost_step";
    public static final String IRON_TELEKINESIS = "irons_spellbooks:telekinesis";
    public static final String IRON_RAISE_HELL = "irons_spellbooks:raise_hell";
    public static final String IRON_FIREBALL = "irons_spellbooks:fireball";
    public static final String IRON_LIGHTNING_BOLT = "irons_spellbooks:lightning_bolt";
    public static final String IRON_LIGHTNING_LANCE = "irons_spellbooks:lightning_lance";
    public static final String IRON_VOLT_STRIKE = "irons_spellbooks:volt_strike";
    public static final String IRON_BURNING_DASH = "irons_spellbooks:burning_dash";
    public static final String IRON_BLACK_HOLE = "irons_spellbooks:black_hole";

    public static final String GEOMANCY_PETRIVISE = "gtbcs_geomancy_plus:petrivise";
    public static final String GTBC_NULLFLARE = "gametechbcs_spellbooks:nullflare";
    public static final String GTBC_FLAMES_REBORN = "gametechbcs_spellbooks:flames_reborn";

    private static final Field CAST_TIME_FIELD = findAbstractSpellField("castTime");

    private FortificationsSpellBalance() {}

    public static boolean isBanned(String spellId) {
        return BannedSpellRegistry.isBanned(spellId);
    }

    public static void applyDefaultConfigOverrides(ModifyDefaultConfigValuesEvent event) {
        AbstractSpell spell = event.getSpell();
        String spellId = spell.getSpellId();

        if (isBanned(spellId)) {
            event.setDefaultValue(IronConfigParameters.ENABLED, false);
            event.setDefaultValue(IronConfigParameters.ALLOW_CRAFTING, false);
        }

        Double cooldownSeconds = SpellBalanceConfig.COOLDOWN_SECONDS.get(spellId);
        if (cooldownSeconds != null) {
            event.setDefaultValue(IronConfigParameters.COOLDOWN_IN_SECONDS, cooldownSeconds);
        }

        Double manaMultiplier = SpellBalanceConfig.MANA_MULTIPLIERS.get(spellId);
        if (manaMultiplier != null) {
            event.setDefaultValue(IronConfigParameters.MANA_MULTIPLIER, manaMultiplier);
        }

        Double powerMultiplier = SpellBalanceConfig.POWER_MULTIPLIERS.get(spellId);
        if (powerMultiplier != null) {
            event.setDefaultValue(IronConfigParameters.POWER_MULTIPLIER, powerMultiplier);
        }

        Integer castTimeTicks = SpellBalanceConfig.CAST_TIME_TICKS.get(spellId);
        if (castTimeTicks != null) {
            setIntField(CAST_TIME_FIELD, spell, castTimeTicks);
        }
    }

    private static Field findAbstractSpellField(String name) {
        try {
            Field field = AbstractSpell.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("Iron's Spells field not found: " + name, exception);
        }
    }

    private static void setIntField(Field field, AbstractSpell spell, int value) {
        try {
            field.setInt(spell, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to update spell field " + field.getName(), exception);
        }
    }
}
