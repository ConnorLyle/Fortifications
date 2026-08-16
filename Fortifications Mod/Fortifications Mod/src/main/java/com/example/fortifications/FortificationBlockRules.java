package com.example.fortifications;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class FortificationBlockRules {
    public static final float DEEPSLATE_HARDNESS = 3.0F;
    public static final float DEEPSLATE_BLAST_RESISTANCE = 6.0F;

    private static final ResourceLocation WARDAY_NEXUS = ResourceLocation.fromNamespaceAndPath("fortifications", "nexus");
    private static final ResourceLocation WARDAY_FORWARD_MARKER = ResourceLocation.fromNamespaceAndPath("fortifications", "forward_marker");
    private static final ResourceLocation WARDAY_ATTACKER_SPAWN = ResourceLocation.fromNamespaceAndPath("fortifications", "attacker_spawn");

    private FortificationBlockRules() {
    }

    public static boolean shouldFortify(BlockState state) {
        if (!FortificationsMod.GLOBAL_ACTIVE || isWardaySetupBlock(state.getBlock())) {
            return false;
        }

        float destroySpeed = state.getBlock().defaultDestroyTime();
        return destroySpeed > 0.0F;
    }

    public static boolean shouldFortify(Block block) {
        return FortificationsMod.GLOBAL_ACTIVE && !isWardaySetupBlock(block) && block.defaultDestroyTime() > 0.0F;
    }

    private static boolean isWardaySetupBlock(Block block) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        return WARDAY_NEXUS.equals(blockId) || WARDAY_FORWARD_MARKER.equals(blockId) || WARDAY_ATTACKER_SPAWN.equals(blockId);
    }
}
