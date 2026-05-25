package com.example.fortifications.mixin;

import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "it.hurts.shatterbyte.reliquified_artifacts.items.feet.SteadfastSpikesItem", remap = false)
public abstract class SteadfastSpikesItemMixin {
    @Redirect(
            method = "constructDefaultRelicTemplate",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/hurts/sskirillss/relics/api/relics/abilities/AbilityTemplate$AbilityTemplateBuilder;rankModifier(ILjava/lang/String;)Lit/hurts/sskirillss/relics/api/relics/abilities/AbilityTemplate$AbilityTemplateBuilder;",
                    ordinal = 1,
                    remap = false
            ),
            remap = false
    )
    private AbilityTemplate.AbilityTemplateBuilder fortifications$removeAnchor(
            AbilityTemplate.AbilityTemplateBuilder builder, int rank, String modifier
    ) {
        return builder;
    }

    @Inject(method = "isAnchorActive", at = @At("HEAD"), cancellable = true)
    private static void fortifications$disableAnchor(Player player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
