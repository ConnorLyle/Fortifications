package com.example.fortifications.spellbalance;

import java.util.Set;

public final class BannedSpellRegistry {

    private static final Set<String> BANNED_SPELL_IDS = Set.of(
            FortificationsSpellBalance.IRON_ANGEL_WING,
            FortificationsSpellBalance.IRON_TOUCH_DIG,
            FortificationsSpellBalance.IRON_POCKET_DIMENSION,
            FortificationsSpellBalance.GTBC_ENSNARE,
            FortificationsSpellBalance.GEOMANCY_SEISMIC_SURF,
            FortificationsSpellBalance.TUNES_SWIFT_MELODY
    );

    private BannedSpellRegistry() {}

    public static boolean isBanned(String spellId) {
        return BANNED_SPELL_IDS.contains(spellId);
    }

    public static Set<String> all() {
        return BANNED_SPELL_IDS;
    }
}
