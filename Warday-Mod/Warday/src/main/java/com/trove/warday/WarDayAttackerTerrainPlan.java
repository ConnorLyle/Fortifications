package com.trove.warday;

import java.util.Optional;

public final class WarDayAttackerTerrainPlan {
    // Pure placement math shared by preview, preparation, and focused verification.
    private WarDayAttackerTerrainPlan() {
    }

    public static int edgeTargetOffset(int halfSizeBlocks, int marginBlocks) {
        int halfSize = Math.max(1, halfSizeBlocks);
        int margin = Math.min(Math.max(1, marginBlocks), Math.max(1, halfSize / 4));
        return Math.max(0, halfSize - margin);
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

    public static SourceAnchor generatedTerrainAnchor(int nexusX, int nexusZ) {
        return generatedTerrainAnchor(nexusX, nexusZ, 0L, 0);
    }

    public static SourceAnchor generatedTerrainAnchor(
            int nexusX,
            int nexusZ,
            long generationSequence,
            int searchAttempt
    ) {
        long mixed = mix64((((long) nexusX) << 32)
                ^ Integer.toUnsignedLong(nexusZ)
                ^ mix64(generationSequence)
                ^ mix64(searchAttempt)
                ^ 0x574152444159L);
        int chunkX = 100_000 + (int) Math.floorMod(mixed, 500_000L);
        int chunkZ = -100_000 - (int) Math.floorMod(mix64(mixed), 500_000L);
        return new SourceAnchor(chunkX * 16 + 8, chunkZ * 16 + 8);
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        return value ^ value >>> 33;
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

    public record CornerLayout(
            int defenderAnchorX,
            int defenderAnchorZ,
            int attackerSpawnX,
            int attackerSpawnZ,
            int marginBlocks
    ) {
    }

    public record SourceAnchor(int x, int z) {
    }
}
