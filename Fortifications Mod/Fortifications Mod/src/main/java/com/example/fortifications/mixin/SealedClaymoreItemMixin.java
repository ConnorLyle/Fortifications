package com.example.fortifications.mixin;

import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "it.hurts.shatterbyte.reliquified_irons_spells_and_spellbooks.items.SealedClaymoreItem", remap = false)
public abstract class SealedClaymoreItemMixin {
    private static final String INITIAL_VALUE_TARGET =
            "Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;initialValue(DD)Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;";
    private static final String TARGET_VALUE_TARGET =
            "Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;targetValue(Lit/hurts/sskirillss/relics/api/scaling_models/ScalingModel;D)Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;";

    @Redirect(
            method = "constructDefaultRelicTemplate",
            at = @At(value = "INVOKE", target = "Lit/hurts/sskirillss/relics/api/relics/abilities/AbilityTemplate$AbilityTemplateBuilder;rankModifier(ILjava/lang/String;)Lit/hurts/sskirillss/relics/api/relics/abilities/AbilityTemplate$AbilityTemplateBuilder;", ordinal = 0, remap = false),
            remap = false
    )
    private AbilityTemplate.AbilityTemplateBuilder fortifications$removeAbsorptionStrike(
            AbilityTemplate.AbilityTemplateBuilder builder, int rank, String modifier
    ) {
        return builder;
    }

    @ModifyArgs(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = INITIAL_VALUE_TARGET, ordinal = 3, remap = false), remap = false)
    private void fortifications$setRespawnTime(Args args) {
        args.set(0, 180.0D);
        args.set(1, 120.0D);
    }

    @ModifyArg(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 3, remap = false), index = 1, remap = false)
    private double fortifications$disableRespawnTimeScaling(double originalTarget) {
        return 120.0D;
    }

    @ModifyArgs(
            method = "constructDefaultRelicTemplate",
            slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=damage_reduction_per_claymore")),
            at = @At(value = "INVOKE", target = INITIAL_VALUE_TARGET, ordinal = 0, remap = false),
            remap = false
    )
    private void fortifications$setGuardReduction(Args args) {
        args.set(0, 0.005D);
        args.set(1, 0.025D);
    }

    @ModifyArg(
            method = "constructDefaultRelicTemplate",
            slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=damage_reduction_per_claymore")),
            at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 0, remap = false),
            index = 1,
            remap = false
    )
    private double fortifications$disableGuardReductionScaling(double originalTarget) {
        return 0.025D;
    }
}
