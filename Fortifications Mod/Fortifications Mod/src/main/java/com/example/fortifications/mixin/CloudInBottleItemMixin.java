package com.example.fortifications.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "it.hurts.shatterbyte.reliquified_artifacts.items.charm.CloudInBottleItem", remap = false)
public abstract class CloudInBottleItemMixin {
    private static final String INITIAL_VALUE_TARGET =
            "Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;initialValue(DD)Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;";
    private static final String TARGET_VALUE_TARGET =
            "Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;targetValue(Lit/hurts/sskirillss/relics/api/scaling_models/ScalingModel;D)Lit/hurts/sskirillss/relics/api/relics/abilities/stats/AbilityStatTemplate$StatTemplateBuilder;";

    @ModifyArgs(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = INITIAL_VALUE_TARGET, ordinal = 0, remap = false), remap = false)
    private void fortifications$setAirJumpCount(Args args) {
        args.set(0, 1.0D);
        args.set(1, 2.0D);
    }

    @ModifyArg(method = "constructDefaultRelicTemplate", at = @At(value = "INVOKE", target = TARGET_VALUE_TARGET, ordinal = 0, remap = false), index = 1, remap = false)
    private double fortifications$disableAirJumpCountScaling(double originalTarget) {
        return 2.0D;
    }
}
