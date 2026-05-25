package com.example.fortifications.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "it.hurts.sskirillss.relics.items.relics.belt.KineticBeltItem", remap = false)
public abstract class KineticBeltItemMixin {
    @ModifyArg(
            method = "constructDefaultRelicTemplate",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;upgradeModifier(Lit/hurts/sskirillss/relics/api/scaling_models/ScalingModel;D)Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;",
                    ordinal = 0,
                    remap = false
            ),
            index = 1,
            remap = false
    )
    private double fortifications$disableKineticBeltSlotScaling(double originalModifier) {
        return 0.0D;
    }
}
