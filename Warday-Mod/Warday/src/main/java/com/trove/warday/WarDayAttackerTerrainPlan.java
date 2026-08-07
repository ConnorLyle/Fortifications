package com.trove.warday;

public final class WarDayAttackerTerrainPlan {
    private WarDayAttackerTerrainPlan() {
    }

    public static int squareChunkCount(int radiusChunks) {
        int diameter = Math.max(0, radiusChunks) * 2 + 1;
        return diameter * diameter;
    }

    public static boolean insideArena(int targetX, int targetZ, int halfSizeBlocks) {
        return targetX >= -halfSizeBlocks && targetX < halfSizeBlocks
                && targetZ >= -halfSizeBlocks && targetZ < halfSizeBlocks;
    }

    public static int clippedColumnCount(
            int spawnChunkX,
            int spawnChunkZ,
            int sourceAnchorX,
            int sourceAnchorZ,
            int targetAnchorX,
            int targetAnchorZ,
            int radiusChunks,
            int halfSizeBlocks
    ) {
        int radius = Math.max(0, radiusChunks);
        int columns = 0;
        for (int chunkX = spawnChunkX - radius; chunkX <= spawnChunkX + radius; chunkX++) {
            for (int chunkZ = spawnChunkZ - radius; chunkZ <= spawnChunkZ + radius; chunkZ++) {
                for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) {
                    for (int z = chunkZ * 16; z < chunkZ * 16 + 16; z++) {
                        int targetX = targetAnchorX + x - sourceAnchorX;
                        int targetZ = targetAnchorZ + z - sourceAnchorZ;
                        if (insideArena(targetX, targetZ, halfSizeBlocks)) {
                            columns++;
                        }
                    }
                }
            }
        }
        return columns;
    }
}
