package com.example.fortifications;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class FortChestBlock extends ChestBlock {
    public static final MapCodec<FortChestBlock> CODEC = simpleCodec(FortChestBlock::new);

    public FortChestBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> FortificationsMod.FORT_CHEST_BLOCK_ENTITY.get());
    }

    @Override
    public MapCodec<? extends FortChestBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(TYPE, ChestType.SINGLE);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction facing,
            BlockState facingState,
            LevelAccessor level,
            BlockPos currentPos,
            BlockPos facingPos
    ) {
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos)
                .setValue(TYPE, ChestType.SINGLE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FortChestBlockEntity(pos, state);
    }
}
