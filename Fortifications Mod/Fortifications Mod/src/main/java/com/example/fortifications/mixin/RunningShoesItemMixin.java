package com.example.fortifications.mixin;

import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "it.hurts.shatterbyte.reliquified_artifacts.items.feet.RunningShoesItem", remap = false)
public abstract class RunningShoesItemMixin {
    @Redirect(
            method = "constructDefaultRelicTemplate",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/hurts/sskirillss/relics/api/relics/abilities/AbilityTemplate$AbilityTemplateBuilder;rankModifier(ILjava/lang/String;)Lit/hurts/sskirillss/relics/api/relics/abilities/AbilityTemplate$AbilityTemplateBuilder;",
                    ordinal = 2,
                    remap = false
            ),
            remap = false
    )
    private AbilityTemplate.AbilityTemplateBuilder fortifications$removeImmortality(
            AbilityTemplate.AbilityTemplateBuilder builder, int rank, String modifier
    ) {
        return builder;
    }

    @Redirect(
            method = "curioTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"
            )
    )
    private boolean fortifications$preventRunningShoesImmortality(Player player, MobEffectInstance effect) {
        return false;
    }
}
