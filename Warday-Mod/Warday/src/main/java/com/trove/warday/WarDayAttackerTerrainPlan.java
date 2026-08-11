package com.trove.warday;

public final class WarDayAttackerTerrainPlan {
    private WarDayAttackerTerrainPlan() {
    }

    public static int edgeTargetOffset(int halfSizeBlocks, int marginBlocks) {
        int halfSize = Math.max(1, halfSizeBlocks);
        int margin = Math.min(Math.max(1, marginBlocks), Math.max(1, halfSize / 4));
        return Math.max(0, halfSize - margin);
    }

    public static SourceWindow sourceWindow(
            int sourceAnchorX,
            int sourceAnchorZ,
            int targetAnchorX,
            int targetAnchorZ,
            int halfSizeBlocks
    ) {
        int halfSize = Math.max(1, halfSizeBlocks);
        int minSourceX = sourceAnchorX - halfSize - targetAnchorX;
        int maxSourceX = sourceAnchorX + halfSize - 1 - targetAnchorX;
        int minSourceZ = sourceAnchorZ - halfSize - targetAnchorZ;
        int maxSourceZ = sourceAnchorZ + halfSize - 1 - targetAnchorZ;
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
