package com.example.fortifications.items;

import java.util.Set;

public final class BannedItemRegistry {
    private static final Set<String> BANNED_ITEMS = Set.of(
            "alexscaves:candy_cane_hook",
            "alexscaves:cloak_of_darkness",
            "alexscaves:hood_of_darkness",
            "alexscaves:raygun",
            "alexscaves:resistor_shield",
            "artifacts:bunny_hoppers",
            "artifacts:charm_of_shrinking",
            "artifacts:helium_flamingo",
            "artifacts:pocket_piston",
            "artifacts:warp_drive",
            "relics:chorus_staff",
            "relics:clot_of_time",
            "relics:experience_disperser",
            "relics:leafy_mantle",
            "reliquified_irons_spells_and_spellbooks:dimension_key",
            "reliquified_irons_spells_and_spellbooks:mirror_of_transgression",
            "reliquified_irons_spells_and_spellbooks:ring_of_blades",
            "reliquified_irons_spells_and_spellbooks:sinner_crown",
            "simplyswords:awakened_lichblade",
            "simplyswords:chompolotl",
            "simplyswords:decaying_relic",
            "simplyswords:slumbering_lichblade",
            "simplyswords:sword_on_a_stick",
            "simplyswords:waking_lichblade",
            "sophisticatedbackpacks:advanced_feeding_upgrade",
            "sophisticatedbackpacks:feeding_upgrade"
    );

    private BannedItemRegistry() {}

    public static boolean isBanned(String itemId) {
        return BANNED_ITEMS.contains(itemId);
    }
}
