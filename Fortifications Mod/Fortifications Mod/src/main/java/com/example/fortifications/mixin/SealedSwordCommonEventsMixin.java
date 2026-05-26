package com.example.fortifications.mixin;

import it.hurts.sskirillss.relics.api.relics.data.AbilityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "it.hurts.shatterbyte.reliquified_irons_spells_and_spellbooks.items.SealedSwordItem$CommonEvents", remap = false)
public abstract class SealedSwordCommonEventsMixin {
    @Redirect(
            method = "onLivingDamagePost",
            at = @At(value = "INVOKE", target = "Lit/hurts/sskirillss/relics/api/relics/data/AbilityData;isRankModifierUnlocked(Ljava/lang/String;)Z", ordinal = 0, remap = false),
            remap = false
    )
    private static boolean fortifications$disableHealStrike(AbilityData abilityData, String modifier) {
        return false;
    }
}
