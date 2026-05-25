package com.example.fortifications.mixin;

import it.hurts.sskirillss.relics.api.relics.data.AbilityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "it.hurts.shatterbyte.reliquified_artifacts.items.feet.SteadfastSpikesItem$SteadfastSpikesEvent", remap = false)
public abstract class SteadfastSpikesEventsMixin {
    @Redirect(
            method = "onLivingKnockBack",
            at = @At(value = "INVOKE", target = "Lit/hurts/sskirillss/relics/api/relics/data/AbilityData;isRankModifierUnlocked(Ljava/lang/String;)Z", ordinal = 1, remap = false),
            remap = false
    )
    private static boolean fortifications$disableAnchorKnockbackResistance(AbilityData abilityData, String modifier) {
        return false;
    }

    @Redirect(
            method = "onLivingSlipping",
            at = @At(value = "INVOKE", target = "Lit/hurts/sskirillss/relics/api/relics/data/AbilityData;isRankModifierUnlocked(Ljava/lang/String;)Z", ordinal = 1, remap = false),
            remap = false
    )
    private static boolean fortifications$disableAnchorFrictionResistance(AbilityData abilityData, String modifier) {
        return false;
    }
}
