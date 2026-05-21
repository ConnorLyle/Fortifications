package com.example.fortifications.mixin;

import io.redspace.ironsspellbooks.spells.ice.FrostStepSpell;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FrostStepSpell.class, remap = false)
public abstract class FrostStepSpellMixin {

    @Inject(method = "getDistance", at = @At("HEAD"), cancellable = true, remap = false)
    private void fortifications$getDistance(int spellLevel, LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(switch (spellLevel) {
            case 1 -> 2.0F;
            case 2 -> 2.5F;
            case 3 -> 3.0F;
            case 4 -> 3.5F;
            case 5 -> 4.0F;
            case 6 -> 4.5F;
            case 7 -> 5.0F;
            default -> 6.0F;
        });
    }
}
