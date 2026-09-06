package com.example.fortifications;

import com.example.fortifications.items.BannedItemRegistry;

public final class BannedItemRegistryTest {
    private static final String SINNER_CROWN =
            "reliquified_irons_spells_and_spellbooks:sinner_crown";

    private BannedItemRegistryTest() {
    }

    public static void main(String[] args) {
        require(BannedItemRegistry.isBanned(SINNER_CROWN), "Sinner Crown must remain banned");
        require(!BannedItemRegistry.isBanned(
                "reliquified_irons_spells_and_spellbooks:hat_of_omniscience"),
                "unrelated Reliquified Iron's Spells items must remain allowed");
        require(!BannedItemRegistry.isBanned("minecraft:air"), "unrelated items must remain allowed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
