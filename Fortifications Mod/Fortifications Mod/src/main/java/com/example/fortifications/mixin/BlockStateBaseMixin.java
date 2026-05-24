package com.example.fortifications.mixin;

import com.example.fortifications.FortificationBlockRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase")
public abstract class BlockStateBaseMixin {

    @Shadow
    protected abstract BlockState asState();

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void fortifications$getDestroySpeed(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (FortificationBlockRules.shouldFortify(asState())) {
            cir.setReturnValue(FortificationBlockRules.DEEPSLATE_HARDNESS);
        }
    }
}
