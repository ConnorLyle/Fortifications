package com.example.fortifications.mixin;

import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "it.hurts.shatterbyte.reliquified_artifacts.items.necklace.ShockPendantItem", remap = false)
public abstract class ShockPendantItemMixin {
    @Redirect(
            method = "constructDefaultRelicTemplate",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/hurts/sskirillss/relics/api/relics/abilities/AbilityTemplate$AbilityTemplateBuilder;rankModifier(ILjava/lang/String;)Lit/hurts/sskirillss/relics/api/relics/abilities/AbilityTemplate$AbilityTemplateBuilder;",
                    ordinal = 0,
                    remap = false
            ),
            remap = false
    )
    private AbilityTemplate.AbilityTemplateBuilder fortifications$removeResistance(
            AbilityTemplate.AbilityTemplateBuilder builder, int rank, String modifier
    ) {
        return builder;
    }
}
