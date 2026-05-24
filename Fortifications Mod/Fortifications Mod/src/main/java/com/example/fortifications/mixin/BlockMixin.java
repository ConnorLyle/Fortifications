package com.example.fortifications.mixin;

import com.example.fortifications.FortificationBlockRules;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(method = "getExplosionResistance", at = @At("HEAD"), cancellable = true)
    private void fortifications$getExplosionResistance(CallbackInfoReturnable<Float> cir) {
        if (FortificationBlockRules.shouldFortify((Block) (Object) this)) {
            cir.setReturnValue(FortificationBlockRules.DEEPSLATE_BLAST_RESISTANCE);
        }
    }
}
