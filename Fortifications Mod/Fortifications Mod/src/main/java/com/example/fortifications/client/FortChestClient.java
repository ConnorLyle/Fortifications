package com.example.fortifications.client;

import com.example.fortifications.FortificationsMod;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FortChestClient {
    public static final Material MATERIAL = new Material(
            Sheets.CHEST_SHEET,
            ResourceLocation.fromNamespaceAndPath(FortificationsMod.MOD_ID, "entity/chest/fort_chest")
    );

    private FortChestClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FortificationsMod.FORT_CHEST_BLOCK_ENTITY.get(), ChestRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new FortChestItemRenderer();
                }
                return this.renderer;
            }
        }, FortificationsMod.FORT_CHEST_ITEM.get());
    }
}
