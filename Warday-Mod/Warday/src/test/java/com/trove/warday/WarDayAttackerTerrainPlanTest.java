package com.trove.warday;

public final class WarDayAttackerTerrainPlanTest {
    private WarDayAttackerTerrainPlanTest() {
    }

    public static void main(String[] args) {
        expect(109, WarDayAttackerTerrainPlan.edgeTargetOffset(125, 16), "default eastern edge inset");
        expect(12, WarDayAttackerTerrainPlan.edgeTargetOffset(16, 16), "small arena scales margin");

        expect(true, WarDayAttackerTerrainPlan.insideArena(-125, -125, 125), "minimum border column");
        expect(true, WarDayAttackerTerrainPlan.insideArena(124, 124, 125), "maximum border column");
        expect(false, WarDayAttackerTerrainPlan.insideArena(125, 0, 125), "positive border excluded");
        expect(false, WarDayAttackerTerrainPlan.insideArena(-126, 0, 125), "below negative border");

        WarDayAttackerTerrainPlan.SourceWindow defaultWindow = WarDayAttackerTerrainPlan.sourceWindow(
                1_000, -1_000, 109, 0, 125);
        expect(766, defaultWindow.minSourceX(), "translated minimum source x");
        expect(1_015, defaultWindow.maxSourceX(), "translated maximum source x");
        expect(-1_125, defaultWindow.minSourceZ(), "translated minimum source z");
        expect(-876, defaultWindow.maxSourceZ(), "translated maximum source z");
        expect(250, defaultWindow.blockWidth(), "full arena width");
        expect(250, defaultWindow.blockDepth(), "full arena depth");
        expect(true, defaultWindow.chunkCount() <= 289, "default guardrail");

        WarDayAttackerTerrainPlan.SourceWindow negativeWindow = WarDayAttackerTerrainPlan.sourceWindow(
                -1, -1, 109, 0, 125);
        expect(-15, negativeWindow.minChunkX(), "negative x floor division");
        expect(0, negativeWindow.maxChunkX(), "negative x maximum chunk");
        expect(-8, negativeWindow.minChunkZ(), "negative z floor division");
        expect(7, negativeWindow.maxChunkZ(), "negative z maximum chunk");
        expect(256, negativeWindow.chunkCount(), "aligned negative window chunk count");
    }

    private static void expect(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }
}
