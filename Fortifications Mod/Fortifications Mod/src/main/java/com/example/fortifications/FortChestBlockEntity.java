package com.example.fortifications;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FortChestBlockEntity extends ChestBlockEntity {
    public FortChestBlockEntity(BlockPos pos, BlockState state) {
        super(FortificationsMod.FORT_CHEST_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fortifications.fort_chest");
    }
}
