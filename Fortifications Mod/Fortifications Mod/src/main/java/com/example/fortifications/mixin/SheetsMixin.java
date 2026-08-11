package com.example.fortifications.mixin;

import com.example.fortifications.FortChestBlockEntity;
import com.example.fortifications.client.FortChestClient;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sheets.class)
public abstract class SheetsMixin {
    @Inject(
            method = "chooseMaterial(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/level/block/state/properties/ChestType;Z)Lnet/minecraft/client/resources/model/Material;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void fortifications$useFortChestTexture(
            BlockEntity blockEntity,
            ChestType chestType,
            boolean holiday,
            CallbackInfoReturnable<Material> callback
    ) {
        if (blockEntity instanceof FortChestBlockEntity) {
            callback.setReturnValue(FortChestClient.MATERIAL);
        }
    }
}
