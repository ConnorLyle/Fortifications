package com.example.fortifications.mixin;

import it.hurts.sskirillss.relics.api.scaling_models.ScalingModel;
import it.hurts.sskirillss.relics.init.RelicsScalingModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "it.hurts.shatterbyte.reliquified_artifacts.items.hands.VampiricGloveItem", remap = false)
public abstract class VampiricGloveItemMixin {
    private static final String INITIAL_VALUE_TARGET =
            "Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;initialValue(DD)Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;";
    private static final String TARGET_VALUE_TARGET =
            "Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;targetValue(Lit/hurts/sskirillss/relics/api/scaling_models/ScalingModel;D)Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;";

    @ModifyArgs(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = INITIAL_VALUE_TARGET, ordinal = 0, remap = false), remap = false)
    private void fortifications$setLifesteal(Args args) {
        args.set(0, 0.01D);
        args.set(1, 0.03D);
    }

    @ModifyArg(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 0, remap = false), index = 0, remap = false)
    private ScalingModel fortifications$useAdditiveLifestealScaling(ScalingModel originalModel) {
        return RelicsScalingModels.ADDITIVE.get();
    }

    @ModifyArg(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 0, remap = false), index = 1, remap = false)
    private double fortifications$setLifestealScaling(double originalTarget) {
        return 0.055D;
    }

    @ModifyArgs(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = INITIAL_VALUE_TARGET, ordinal = 1, remap = false), remap = false)
    private void fortifications$setStreakHealBonus(Args args) {
        args.set(0, 0.0025D);
        args.set(1, 0.005D);
    }

    @ModifyArg(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 1, remap = false), index = 1, remap = false)
    private double fortifications$disableStreakHealScaling(double originalTarget) {
        return 0.005D;
    }

    @ModifyArgs(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = INITIAL_VALUE_TARGET, ordinal = 4, remap = false), remap = false)
    private void fortifications$setKillHeal(Args args) {
        args.set(0, 0.05D);
        args.set(1, 0.10D);
    }

    @ModifyArg(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 4, remap = false), index = 1, remap = false)
    private double fortifications$disableKillHealScaling(double originalTarget) {
        return 0.10D;
    }
}
