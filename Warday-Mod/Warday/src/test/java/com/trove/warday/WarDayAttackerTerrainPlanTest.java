package com.trove.warday;

import java.util.Optional;

public final class WarDayAttackerTerrainPlanTest {
    private WarDayAttackerTerrainPlanTest() {
    }

    public static void main(String[] args) {
        expect(109, WarDayAttackerTerrainPlan.edgeTargetOffset(125, 16), "default eastern edge inset");
        expect(12, WarDayAttackerTerrainPlan.edgeTargetOffset(16, 16), "small arena scales margin");
        WarDayAttackerTerrainPlan.CornerLayout defaultLayout = WarDayAttackerTerrainPlan.cornerLayout(
                -31, 32, -15, 16, 125, 16).orElseThrow();
        expect(77, defaultLayout.defenderAnchorX(), "defender anchor fits northeast corner");
        expect(-94, defaultLayout.defenderAnchorZ(), "defender anchor fits northern edge");
        expect(-109, defaultLayout.attackerSpawnX(), "attacker uses opposite western edge");
        expect(109, defaultLayout.attackerSpawnZ(), "attacker uses opposite southern edge");
        expect(16, defaultLayout.marginBlocks(), "default corner margin");

        WarDayAttackerTerrainPlan.CornerLayout largeLayout = WarDayAttackerTerrainPlan.cornerLayout(
                -119, 120, -119, 120, 125, 16).orElseThrow();
        expect(5, largeLayout.marginBlocks(), "large base reduces margin to remain fully inside arena");
        expect(-109, largeLayout.attackerSpawnX(), "large base does not push attacker into the border");
        expect(109, largeLayout.attackerSpawnZ(), "large base keeps the standard opposite corner");
        expect(Optional.empty(), WarDayAttackerTerrainPlan.cornerLayout(
                -124, 124, -15, 16, 125, 16), "base requiring the complete arena is rejected");

        WarDayAttackerTerrainPlan.SourceAnchor terrainAnchorA =
                WarDayAttackerTerrainPlan.generatedTerrainAnchor(1234, -5678);
        WarDayAttackerTerrainPlan.SourceAnchor terrainAnchorB =
                WarDayAttackerTerrainPlan.generatedTerrainAnchor(1234, -5678);
        expect(terrainAnchorA, terrainAnchorB, "generated terrain source is preview-stable");
        expect(true, Math.abs(terrainAnchorA.x()) > 1_000_000, "generated terrain source stays remote from arena x");
        expect(true, Math.abs(terrainAnchorA.z()) > 1_000_000, "generated terrain source stays remote from arena z");
        expect(false, terrainAnchorA.equals(
                        WarDayAttackerTerrainPlan.generatedTerrainAnchor(1234, -5678, 1L, 0)),
                "successful preparation advances to a fresh source region");
        expect(false, terrainAnchorA.equals(
                        WarDayAttackerTerrainPlan.generatedTerrainAnchor(1234, -5678, 0L, 1)),
                "bounded biome-search attempts use different remote regions");

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
