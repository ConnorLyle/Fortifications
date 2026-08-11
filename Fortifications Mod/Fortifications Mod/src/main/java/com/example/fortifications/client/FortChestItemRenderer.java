package com.example.fortifications.client;

import com.example.fortifications.FortChestBlockEntity;
import com.example.fortifications.FortificationsMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class FortChestItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final FortChestBlockEntity chest = new FortChestBlockEntity(
            BlockPos.ZERO,
            FortificationsMod.FORT_CHEST.get().defaultBlockState()
    );

    public FortChestItemRenderer() {
        super(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels()
        );
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        Minecraft.getInstance()
                .getBlockEntityRenderDispatcher()
                .renderItem(this.chest, poseStack, buffer, packedLight, packedOverlay);
    }
}
