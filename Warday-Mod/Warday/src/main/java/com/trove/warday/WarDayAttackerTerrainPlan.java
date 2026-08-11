package com.trove.warday;

import java.util.OptionalInt;

public final class WarDayAttackerTerrainPlan {
    private WarDayAttackerTerrainPlan() {
    }

    public static int edgeTargetOffset(int halfSizeBlocks, int marginBlocks) {
        int halfSize = Math.max(1, halfSizeBlocks);
        int margin = Math.min(Math.max(1, marginBlocks), Math.max(1, halfSize / 4));
        return Math.max(0, halfSize - margin);
    }

    public static OptionalInt automaticSpawnX(
            int defenderClaimMaxX,
            int halfSizeBlocks,
            int desiredGapBlocks,
            int borderMarginBlocks
    ) {
        int maximumSafeX = edgeTargetOffset(halfSizeBlocks, borderMarginBlocks);
        int firstOutsideClaim = defenderClaimMaxX + 1;
        if (firstOutsideClaim > maximumSafeX) {
            return OptionalInt.empty();
        }
        int preferredX = defenderClaimMaxX + Math.max(1, desiredGapBlocks);
        return OptionalInt.of(Math.min(preferredX, maximumSafeX));
    }

    public static SourceWindow sourceWindow(
            int sourceAnchorX,
            int sourceAnchorZ,
            int targetAnchorX,
            int targetAnchorZ,
            int halfSizeBlocks
    ) {
        return rotatedSourceWindow(
                sourceAnchorX, sourceAnchorZ, targetAnchorX, targetAnchorZ, halfSizeBlocks, 0);
    }

    public static SourceWindow rotatedSourceWindow(
            int sourceAnchorX,
            int sourceAnchorZ,
            int targetAnchorX,
            int targetAnchorZ,
            int halfSizeBlocks,
            int clockwiseQuarterTurns
    ) {
        int halfSize = Math.max(1, halfSizeBlocks);
        int minTargetOffsetX = -halfSize - targetAnchorX;
        int maxTargetOffsetX = halfSize - 1 - targetAnchorX;
        int minTargetOffsetZ = -halfSize - targetAnchorZ;
        int maxTargetOffsetZ = halfSize - 1 - targetAnchorZ;
        int minSourceOffsetX = Integer.MAX_VALUE;
        int maxSourceOffsetX = Integer.MIN_VALUE;
        int minSourceOffsetZ = Integer.MAX_VALUE;
        int maxSourceOffsetZ = Integer.MIN_VALUE;
        int turns = Math.floorMod(clockwiseQuarterTurns, 4);
        for (int targetOffsetX : new int[]{minTargetOffsetX, maxTargetOffsetX}) {
            for (int targetOffsetZ : new int[]{minTargetOffsetZ, maxTargetOffsetZ}) {
                int sourceOffsetX;
                int sourceOffsetZ;
                switch (turns) {
                    case 1 -> {
                        sourceOffsetX = targetOffsetZ;
                        sourceOffsetZ = -targetOffsetX;
                    }
                    case 2 -> {
                        sourceOffsetX = -targetOffsetX;
                        sourceOffsetZ = -targetOffsetZ;
                    }
                    case 3 -> {
                        sourceOffsetX = -targetOffsetZ;
                        sourceOffsetZ = targetOffsetX;
                    }
                    default -> {
                        sourceOffsetX = targetOffsetX;
                        sourceOffsetZ = targetOffsetZ;
                    }
                }
                minSourceOffsetX = Math.min(minSourceOffsetX, sourceOffsetX);
                maxSourceOffsetX = Math.max(maxSourceOffsetX, sourceOffsetX);
                minSourceOffsetZ = Math.min(minSourceOffsetZ, sourceOffsetZ);
                maxSourceOffsetZ = Math.max(maxSourceOffsetZ, sourceOffsetZ);
            }
        }
        int minSourceX = sourceAnchorX + minSourceOffsetX;
        int maxSourceX = sourceAnchorX + maxSourceOffsetX;
        int minSourceZ = sourceAnchorZ + minSourceOffsetZ;
        int maxSourceZ = sourceAnchorZ + maxSourceOffsetZ;
        return new SourceWindow(
                Math.floorDiv(minSourceX, 16),
                Math.floorDiv(maxSourceX, 16),
                Math.floorDiv(minSourceZ, 16),
                Math.floorDiv(maxSourceZ, 16),
                minSourceX,
                maxSourceX,
                minSourceZ,
                maxSourceZ
        );
    }

    public static boolean insideArena(int targetX, int targetZ, int halfSizeBlocks) {
        return targetX >= -halfSizeBlocks && targetX < halfSizeBlocks
                && targetZ >= -halfSizeBlocks && targetZ < halfSizeBlocks;
    }

    public record SourceWindow(
            int minChunkX,
            int maxChunkX,
            int minChunkZ,
            int maxChunkZ,
            int minSourceX,
            int maxSourceX,
            int minSourceZ,
            int maxSourceZ
    ) {
        public int chunkCount() {
            return (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        }

        public int blockWidth() {
            return maxSourceX - minSourceX + 1;
        }

        public int blockDepth() {
            return maxSourceZ - minSourceZ + 1;
        }
    }
}
