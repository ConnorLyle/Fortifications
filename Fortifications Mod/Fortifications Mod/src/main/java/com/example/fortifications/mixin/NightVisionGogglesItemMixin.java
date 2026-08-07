package com.example.fortifications.mixin;

import it.hurts.sskirillss.relics.api.scaling_models.ScalingModel;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "it.hurts.shatterbyte.reliquified_artifacts.items.hat.NightVisionGogglesItem", remap = false)
public abstract class NightVisionGogglesItemMixin {
    private static final String INITIAL_VALUE_TARGET =
            "Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;initialValue(DD)Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;";
    private static final String TARGET_VALUE_TARGET =
            "Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;targetValue(Lit/hurts/sskirillss/relics/api/scaling_models/ScalingModel;D)Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;";

    @ModifyArgs(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = INITIAL_VALUE_TARGET, ordinal = 1, remap = false), remap = false)
    private void fortifications$setEvasionChance(Args args) {
        args.set(0, 0.01D);
        args.set(1, 0.05D);
    }

    @ModifyArg(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 1, remap = false), index = 0, remap = false)
    private ScalingModel fortifications$useAdditiveEvasionScaling(ScalingModel originalModel) {
        return RelicsScalingModels.ADDITIVE.get();
    }

    @ModifyArg(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 1, remap = false), index = 1, remap = false)
    private double fortifications$setEvasionScaling(double originalTarget) {
        return 0.075D;
    }

    @ModifyArgs(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = INITIAL_VALUE_TARGET, ordinal = 2, remap = false), remap = false)
    private void fortifications$setAmbushDamage(Args args) {
        args.set(0, 0.05D);
        args.set(1, 0.15D);
    }

    @ModifyArg(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 2, remap = false), index = 0, remap = false)
    private ScalingModel fortifications$useAdditiveAmbushScaling(ScalingModel originalModel) {
        return RelicsScalingModels.ADDITIVE.get();
    }

    @ModifyArg(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 2, remap = false), index = 1, remap = false)
    private double fortifications$setAmbushScaling(double originalTarget) {
        return 0.175D;
    }
}
