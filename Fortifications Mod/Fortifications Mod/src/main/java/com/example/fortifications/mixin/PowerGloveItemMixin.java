package com.example.fortifications.mixin;

import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.scaling_models.ScalingModel;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.ModifyArgs;

@Mixin(targets = "it.hurts.shatterbyte.reliquified_artifacts.items.hands.PowerGloveItem", remap = false)
public abstract class PowerGloveItemMixin {
    private static final String INITIAL_VALUE_TARGET =
            "Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;initialValue(DD)Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;";
    private static final String TARGET_VALUE_TARGET =
            "Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;targetValue(Lit/hurts/sskirillss/relics/api/scaling_models/ScalingModel;D)Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;";

    @Redirect(
            method = "constructDefaultRelicTemplate",
            at = @At(value = "INVOKE", target = "Lit/hurts/sskirillss/relics/api/relics/abilities/AbilityTemplate$AbilityTemplateBuilder;rankModifier(ILjava/lang/String;)Lit/hurts/sskirillss/relics/api/relics/abilities/AbilityTemplate$AbilityTemplateBuilder;", ordinal = 0, remap = false),
            remap = false
    )
    private AbilityTemplate.AbilityTemplateBuilder fortifications$removeShieldBreak(
            AbilityTemplate.AbilityTemplateBuilder builder, int rank, String modifier
    ) {
        return builder;
    }

    @Redirect(
            method = "constructDefaultRelicTemplate",
            at = @At(value = "INVOKE", target = "Lit/hurts/sskirillss/relics/api/relics/abilities/AbilityTemplate$AbilityTemplateBuilder;rankModifier(ILjava/lang/String;)Lit/hurts/sskirillss/relics/api/relics/abilities/AbilityTemplate$AbilityTemplateBuilder;", ordinal = 1, remap = false),
            remap = false
    )
    private AbilityTemplate.AbilityTemplateBuilder fortifications$removeArmorPierce(
            AbilityTemplate.AbilityTemplateBuilder builder, int rank, String modifier
    ) {
        return builder;
    }

    @ModifyArgs(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = INITIAL_VALUE_TARGET, ordinal = 0, remap = false), remap = false)
    private void fortifications$setHitsRequired(Args args) {
        args.set(0, 9.0D);
        args.set(1, 7.0D);
    }

    @ModifyArg(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 0, remap = false), index = 1, remap = false)
    private double fortifications$disableHitsRequiredScaling(double originalTarget) {
        return 7.0D;
    }

    @ModifyArgs(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = INITIAL_VALUE_TARGET, ordinal = 1, remap = false), remap = false)
    private void fortifications$setPowerDamageBonus(Args args) {
        args.set(0, 0.10D);
        args.set(1, 0.15D);
    }

    @ModifyArg(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 1, remap = false), index = 0, remap = false)
    private ScalingModel fortifications$useAdditivePowerDamageScaling(ScalingModel originalModel) {
        return RelicsScalingModels.ADDITIVE.get();
    }

    @ModifyArg(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 1, remap = false), index = 1, remap = false)
    private double fortifications$setPowerDamageScaling(double originalTarget) {
        return 0.25D;
    }
}
