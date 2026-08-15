package com.trove.warday;

import java.util.NoSuchElementException;

final class WarDayBiomeSearchGrid {
    private static final int[][] PRECHECK_POINTS = {
            {0, 0}, {1, 0}, {2, 0},
            {0, 1},         {2, 1},
            {0, 2}, {1, 2}, {2, 2}
    };

    private WarDayBiomeSearchGrid() {
    }

    static Cursor cursor(int minBlockX, int maxBlockX, int minBlockZ, int maxBlockZ) {
        return new Cursor(
                Math.floorDiv(minBlockX, 4),
                Math.floorDiv(maxBlockX, 4),
                Math.floorDiv(minBlockZ, 4),
                Math.floorDiv(maxBlockZ, 4)
        );
    }

    record Sample(int blockX, int blockZ, boolean precheck) {
    }

    static final class Cursor {
        private final int minQuartX;
        private final int maxQuartX;
        private final int minQuartZ;
        private final int maxQuartZ;
        private int precheckIndex;
        private int exactQuartX;
        private int exactQuartZ;
        private boolean exactStarted;

        private Cursor(int minQuartX, int maxQuartX, int minQuartZ, int maxQuartZ) {
            this.minQuartX = minQuartX;
            this.maxQuartX = maxQuartX;
            this.minQuartZ = minQuartZ;
            this.maxQuartZ = maxQuartZ;
            this.exactQuartX = minQuartX;
            this.exactQuartZ = minQuartZ;
        }

        boolean hasNext() {
            return precheckIndex < PRECHECK_POINTS.length
                    || !exactStarted
                    || exactQuartX <= maxQuartX;
        }

        Sample next() {
            if (precheckIndex < PRECHECK_POINTS.length) {
                int[] point = PRECHECK_POINTS[precheckIndex++];
                int quartX = interpolate(minQuartX, maxQuartX, point[0]);
                int quartZ = interpolate(minQuartZ, maxQuartZ, point[1]);
                return new Sample(quartToCenterBlock(quartX), quartToCenterBlock(quartZ), true);
            }

            if (!exactStarted) {
                exactStarted = true;
            }
            if (exactQuartX > maxQuartX) {
                throw new NoSuchElementException("Biome search grid exhausted");
            }

            Sample sample = new Sample(
                    quartToCenterBlock(exactQuartX),
                    quartToCenterBlock(exactQuartZ),
                    false
            );
            exactQuartZ++;
            if (exactQuartZ > maxQuartZ) {
                exactQuartZ = minQuartZ;
                exactQuartX++;
            }
            return sample;
        }

        private static int interpolate(int min, int max, int index) {
            return switch (index) {
                case 0 -> min;
                case 1 -> min + Math.floorDiv(max - min, 2);
                case 2 -> max;
                default -> throw new IllegalArgumentException("Expected a three-point grid index");
            };
        }

        private static int quartToCenterBlock(int quart) {
            return quart * 4 + 2;
        }
    }
}
