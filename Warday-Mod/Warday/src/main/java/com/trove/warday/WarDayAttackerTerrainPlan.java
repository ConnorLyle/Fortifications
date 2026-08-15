package com.trove.warday;

import java.util.Optional;
import java.util.List;

public final class WarDayAttackerTerrainPlan {
    // Pure placement math shared by preview, preparation, and focused verification.
    private WarDayAttackerTerrainPlan() {
    }

    public static int edgeTargetOffset(int halfSizeBlocks, int marginBlocks) {
        int halfSize = Math.max(1, halfSizeBlocks);
        int margin = Math.min(Math.max(1, marginBlocks), Math.max(1, halfSize / 4));
        return Math.max(0, halfSize - margin);
    }

    public static List<CornerSpawn> cornerSpawnTargets(int halfSizeBlocks, int marginBlocks) {
        int edge = edgeTargetOffset(halfSizeBlocks, marginBlocks);
        return List.of(
                new CornerSpawn("Northwest", -edge, -edge),
                new CornerSpawn("Northeast", edge, -edge),
                new CornerSpawn("Southwest", -edge, edge),
                new CornerSpawn("Southeast", edge, edge)
        );
    }

    public static int fallbackCornerIndex(int deathCount, int cornerCount) {
        if (cornerCount < 1) {
            throw new IllegalArgumentException("At least one corner is required");
        }
        return Math.floorMod(Math.max(1, deathCount) - 1, cornerCount);
    }

    public static Optional<CornerLayout> cornerLayout(
            int relativeMinX,
            int relativeMaxX,
            int relativeMinZ,
            int relativeMaxZ,
            int halfSizeBlocks,
            int desiredMarginBlocks
    ) {
        int halfSize = Math.max(1, halfSizeBlocks);
        int arenaSize = halfSize * 2;
        int width = relativeMaxX - relativeMinX + 1;
        int depth = relativeMaxZ - relativeMinZ + 1;
        if (width <= 0 || depth <= 0 || width > arenaSize - 2 || depth > arenaSize - 2) {
            return Optional.empty();
        }

        int maximumMargin = Math.min((arenaSize - width) / 2, (arenaSize - depth) / 2);
        int margin = Math.max(1, Math.min(Math.max(1, desiredMarginBlocks), maximumMargin));
        int edge = halfSize - margin;
        int spawnEdge = edgeTargetOffset(halfSize, desiredMarginBlocks);
        int defenderAnchorX = edge - relativeMaxX;
        int defenderAnchorZ = -edge - relativeMinZ;
        return Optional.of(new CornerLayout(
                defenderAnchorX,
                defenderAnchorZ,
                -spawnEdge,
                spawnEdge,
                margin
        ));
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

    public static int maximumArenaSearchRadius(int preferredX, int preferredZ, int halfSizeBlocks) {
        int halfSize = Math.max(1, halfSizeBlocks);
        int min = -halfSize;
        int max = halfSize - 1;
        return Math.max(
                Math.max(Math.abs(preferredX - min), Math.abs(preferredX - max)),
                Math.max(Math.abs(preferredZ - min), Math.abs(preferredZ - max))
        );
    }

    public static int nearestRingSize(int distance) {
        if (distance < 0) {
            throw new IllegalArgumentException("Distance cannot be negative");
        }
        return distance == 0 ? 1 : distance * 8;
    }

    public static ColumnOffset nearestRingOffset(int distance, int index) {
        int ringSize = nearestRingSize(distance);
        if (index < 0 || index >= ringSize) {
            throw new IllegalArgumentException("Ring index is outside the requested distance");
        }
        if (distance == 0) {
            return new ColumnOffset(0, 0);
        }

        int topLength = distance * 2 + 1;
        if (index < topLength) {
            return new ColumnOffset(-distance + index, -distance);
        }
        index -= topLength;

        int sideLength = distance * 2;
        if (index < sideLength) {
            return new ColumnOffset(distance, -distance + 1 + index);
        }
        index -= sideLength;
        if (index < sideLength) {
            return new ColumnOffset(distance - 1 - index, distance);
        }
        index -= sideLength;
        return new ColumnOffset(-distance, distance - 1 - index);
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

    public record CornerLayout(
            int defenderAnchorX,
            int defenderAnchorZ,
            int attackerSpawnX,
            int attackerSpawnZ,
            int marginBlocks
    ) {
    }

    public record CornerSpawn(String name, int x, int z) {
    }

    public record ColumnOffset(int x, int z) {
    }

}
