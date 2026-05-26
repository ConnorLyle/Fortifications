package com.example.fortifications.mixin;

import it.hurts.sskirillss.relics.api.relics.data.AbilityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "it.hurts.shatterbyte.reliquified_artifacts.items.necklace.ShockPendantItem$CommonEvents", remap = false)
public abstract class ShockPendantCommonEventsMixin {
    @Redirect(
            method = "onLivingIncomingDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/hurts/sskirillss/relics/api/relics/data/AbilityData;isRankModifierUnlocked(Ljava/lang/String;)Z",
                    ordinal = 0,
                    remap = false
            ),
            remap = false
    )
    private static boolean fortifications$disableResistance(AbilityData abilityData, String modifier) {
        return false;
    }
}
