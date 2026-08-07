package com.example.fortifications.mixin;

import it.hurts.sskirillss.relics.api.relics.data.AbilityRankModifierData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "it.hurts.shatterbyte.reliquified_artifacts.items.hands.PowerGloveItem$CommonEvents", remap = false)
public abstract class PowerGloveCommonEventsMixin {
    @Redirect(
            method = "onLivingShieldBlock",
            at = @At(value = "INVOKE", target = "Lit/hurts/sskirillss/relics/api/relics/data/AbilityRankModifierData;isEnabled()Z", remap = false),
            remap = false
    )
    private static boolean fortifications$disableShieldBreak(AbilityRankModifierData modifierData) {
        return false;
    }

    @Redirect(
            method = "onLivingIncomingDamageDealing",
            at = @At(value = "INVOKE", target = "Lit/hurts/sskirillss/relics/api/relics/data/AbilityRankModifierData;isEnabled()Z", remap = false),
            remap = false
    )
    private static boolean fortifications$disableArmorPierce(AbilityRankModifierData modifierData) {
        return false;
    }
}
