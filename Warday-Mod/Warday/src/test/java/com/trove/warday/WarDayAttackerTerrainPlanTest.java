package com.trove.warday;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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

        var corners = WarDayAttackerTerrainPlan.cornerSpawnTargets(128, 16);
        expect(4, corners.size(), "four attacker corner targets");
        expect(new WarDayAttackerTerrainPlan.CornerSpawn("Northwest", -112, -112), corners.get(0), "northwest target");
        expect(new WarDayAttackerTerrainPlan.CornerSpawn("Northeast", 112, -112), corners.get(1), "northeast target");
        expect(new WarDayAttackerTerrainPlan.CornerSpawn("Southwest", -112, 112), corners.get(2), "southwest target");
        expect(new WarDayAttackerTerrainPlan.CornerSpawn("Southeast", 112, 112), corners.get(3), "southeast target");
        expect(0, WarDayAttackerTerrainPlan.fallbackCornerIndex(1, 4), "first death fallback");
        expect(3, WarDayAttackerTerrainPlan.fallbackCornerIndex(4, 4), "fourth death fallback");
        expect(0, WarDayAttackerTerrainPlan.fallbackCornerIndex(5, 4), "fallback wraps corners");

        expect(true, WarDayAttackerTerrainPlan.insideArena(-125, -125, 125), "minimum border column");
        expect(true, WarDayAttackerTerrainPlan.insideArena(124, 124, 125), "maximum border column");
        expect(false, WarDayAttackerTerrainPlan.insideArena(125, 0, 125), "positive border excluded");
        expect(false, WarDayAttackerTerrainPlan.insideArena(-126, 0, 125), "below negative border");

        expect(240, WarDayAttackerTerrainPlan.maximumArenaSearchRadius(112, -112, 128),
                "corner fallback reaches the far side of the complete arena");
        expect(128, WarDayAttackerTerrainPlan.maximumArenaSearchRadius(0, 0, 128),
                "center fallback reaches the negative arena edge");
        Set<String> searchedArenaColumns = new HashSet<>();
        int maximumRadius = WarDayAttackerTerrainPlan.maximumArenaSearchRadius(112, -112, 128);
        for (int distance = 0; distance <= maximumRadius; distance++) {
            int ringSize = WarDayAttackerTerrainPlan.nearestRingSize(distance);
            for (int index = 0; index < ringSize; index++) {
                WarDayAttackerTerrainPlan.ColumnOffset offset =
                        WarDayAttackerTerrainPlan.nearestRingOffset(distance, index);
                expect(distance, Math.max(Math.abs(offset.x()), Math.abs(offset.z())),
                        "ring offset remains at its requested distance");
                int x = 112 + offset.x();
                int z = -112 + offset.z();
                if (WarDayAttackerTerrainPlan.insideArena(x, z, 128)) {
                    searchedArenaColumns.add(x + "," + z);
                }
            }
        }
        expect(256 * 256, searchedArenaColumns.size(), "fallback visits every arena column");

        WarDayAttackerTerrainPlan.SourceWindow defaultWindow = WarDayAttackerTerrainPlan.sourceWindow(
                1_000, -1_000, 0, 0, 128);
        expect(872, defaultWindow.minSourceX(), "translated minimum source x");
        expect(1_127, defaultWindow.maxSourceX(), "translated maximum source x");
        expect(-1_128, defaultWindow.minSourceZ(), "translated minimum source z");
        expect(-873, defaultWindow.maxSourceZ(), "translated maximum source z");
        expect(256, defaultWindow.blockWidth(), "full arena width");
        expect(256, defaultWindow.blockDepth(), "full arena depth");
        expect(true, defaultWindow.chunkCount() <= 289, "default guardrail");

        for (int turns = 0; turns < 4; turns++) {
            WarDayAttackerTerrainPlan.SourceWindow rotated = WarDayAttackerTerrainPlan.rotatedSourceWindow(
                    1_000, -1_000, 0, 0, 128, turns);
            expect(256, rotated.blockWidth(), "rotated full width " + turns);
            expect(256, rotated.blockDepth(), "rotated full depth " + turns);
            expect(true, rotated.chunkCount() <= 289, "rotated default guardrail " + turns);
        }
        WarDayAttackerTerrainPlan.SourceWindow clockwise = WarDayAttackerTerrainPlan.rotatedSourceWindow(
                1_000, -1_000, 0, 0, 128, 1);
        expect(-1_127, clockwise.minSourceZ(), "clockwise even-size minimum z");
        expect(-872, clockwise.maxSourceZ(), "clockwise even-size maximum z");

        WarDayAttackerTerrainPlan.SourceWindow negativeWindow = WarDayAttackerTerrainPlan.sourceWindow(
                -1, -1, 0, 0, 128);
        expect(-9, negativeWindow.minChunkX(), "negative x floor division");
        expect(7, negativeWindow.maxChunkX(), "negative x maximum chunk");
        expect(-9, negativeWindow.minChunkZ(), "negative z floor division");
        expect(7, negativeWindow.maxChunkZ(), "negative z maximum chunk");
        expect(289, negativeWindow.chunkCount(), "unaligned nexus-centered window chunk count");
    }

    private static void expect(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }
}
