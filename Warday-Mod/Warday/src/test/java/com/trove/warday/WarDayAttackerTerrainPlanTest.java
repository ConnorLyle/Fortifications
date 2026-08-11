package com.trove.warday;

import java.util.OptionalInt;

public final class WarDayAttackerTerrainPlanTest {
    private WarDayAttackerTerrainPlanTest() {
    }

    public static void main(String[] args) {
        expect(109, WarDayAttackerTerrainPlan.edgeTargetOffset(125, 16), "default eastern edge inset");
        expect(12, WarDayAttackerTerrainPlan.edgeTargetOffset(16, 16), "small arena scales margin");
        expect(OptionalInt.of(48), WarDayAttackerTerrainPlan.automaticSpawnX(32, 125, 16, 16),
                "preferred claim-outskirts gap");
        expect(OptionalInt.of(109), WarDayAttackerTerrainPlan.automaticSpawnX(100, 125, 16, 16),
                "gap clamps inside border");
        expect(OptionalInt.of(109), WarDayAttackerTerrainPlan.automaticSpawnX(108, 125, 16, 16),
                "minimum outside-claim space");
        expect(OptionalInt.empty(), WarDayAttackerTerrainPlan.automaticSpawnX(109, 125, 16, 16),
                "claim leaves no safe attacker side");

        expect(true, WarDayAttackerTerrainPlan.insideArena(-125, -125, 125), "minimum border column");
        expect(true, WarDayAttackerTerrainPlan.insideArena(124, 124, 125), "maximum border column");
        expect(false, WarDayAttackerTerrainPlan.insideArena(125, 0, 125), "positive border excluded");
        expect(false, WarDayAttackerTerrainPlan.insideArena(-126, 0, 125), "below negative border");

        WarDayAttackerTerrainPlan.SourceWindow defaultWindow = WarDayAttackerTerrainPlan.sourceWindow(
                1_000, -1_000, 0, 0, 125);
        expect(875, defaultWindow.minSourceX(), "translated minimum source x");
        expect(1_124, defaultWindow.maxSourceX(), "translated maximum source x");
        expect(-1_125, defaultWindow.minSourceZ(), "translated minimum source z");
        expect(-876, defaultWindow.maxSourceZ(), "translated maximum source z");
        expect(250, defaultWindow.blockWidth(), "full arena width");
        expect(250, defaultWindow.blockDepth(), "full arena depth");
        expect(true, defaultWindow.chunkCount() <= 289, "default guardrail");

        for (int turns = 0; turns < 4; turns++) {
            WarDayAttackerTerrainPlan.SourceWindow rotated = WarDayAttackerTerrainPlan.rotatedSourceWindow(
                    1_000, -1_000, 0, 0, 125, turns);
            expect(250, rotated.blockWidth(), "rotated full width " + turns);
            expect(250, rotated.blockDepth(), "rotated full depth " + turns);
            expect(true, rotated.chunkCount() <= 289, "rotated default guardrail " + turns);
        }
        WarDayAttackerTerrainPlan.SourceWindow clockwise = WarDayAttackerTerrainPlan.rotatedSourceWindow(
                1_000, -1_000, 0, 0, 125, 1);
        expect(-1_124, clockwise.minSourceZ(), "clockwise even-size minimum z");
        expect(-875, clockwise.maxSourceZ(), "clockwise even-size maximum z");

        WarDayAttackerTerrainPlan.SourceWindow negativeWindow = WarDayAttackerTerrainPlan.sourceWindow(
                -1, -1, 0, 0, 125);
        expect(-8, negativeWindow.minChunkX(), "negative x floor division");
        expect(7, negativeWindow.maxChunkX(), "negative x maximum chunk");
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
