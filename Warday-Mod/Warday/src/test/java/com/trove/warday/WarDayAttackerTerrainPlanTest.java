package com.trove.warday;

public final class WarDayAttackerTerrainPlanTest {
    private WarDayAttackerTerrainPlanTest() {
    }

    public static void main(String[] args) {
        expect(1, WarDayAttackerTerrainPlan.squareChunkCount(0), "radius zero");
        expect(9, WarDayAttackerTerrainPlan.squareChunkCount(1), "radius one");
        expect(289, WarDayAttackerTerrainPlan.squareChunkCount(8), "default radius");

        expect(true, WarDayAttackerTerrainPlan.insideArena(-125, -125, 125), "minimum border column");
        expect(true, WarDayAttackerTerrainPlan.insideArena(124, 124, 125), "maximum border column");
        expect(false, WarDayAttackerTerrainPlan.insideArena(125, 0, 125), "positive border excluded");
        expect(false, WarDayAttackerTerrainPlan.insideArena(-126, 0, 125), "below negative border");

        expect(256, WarDayAttackerTerrainPlan.clippedColumnCount(0, 0, 0, 0, 0, 0, 0, 125), "one full chunk");
        expect(250 * 250, WarDayAttackerTerrainPlan.clippedColumnCount(0, 0, 0, 0, 0, 0, 8, 125), "default radius clipped to arena");
        expect(250 * 250, WarDayAttackerTerrainPlan.clippedColumnCount(-7, -7, -112, -112, 0, 0, 8, 125), "negative chunks and translated anchor");
        expect(128 * 250, WarDayAttackerTerrainPlan.clippedColumnCount(0, 0, 0, 0, 125, 0, 8, 125), "window clipped at positive edge");
    }

    private static void expect(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }
}
