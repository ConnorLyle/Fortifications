package com.example.fortifications.mixin;

import io.redspace.ironsspellbooks.spells.ender.PortalSpell;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PortalSpell.class, remap = false)
public abstract class PortalSpellMixin {

    @Inject(method = "getCastDistance", at = @At("HEAD"), cancellable = true, remap = false)
    private void fortifications$getCastDistance(int spellLevel, LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(switch (spellLevel) {
            case 1 -> 12.0F;
            case 2 -> 24.0F;
            default -> 36.0F;
        });
    }
}
