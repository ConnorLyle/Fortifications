package com.trove.warday;

import java.util.HashSet;
import java.util.Set;

public final class WarDayBiomeSearchGridTest {
    private WarDayBiomeSearchGridTest() {
    }

    public static void main(String[] args) {
        verifiesCompleteExactCoverageAfterPrecheck();
        verifiesNegativeCoordinateCoverage();
        verifiesCursorCanResumeAcrossSmallBatches();
    }

    private static void verifiesCompleteExactCoverageAfterPrecheck() {
        WarDayBiomeSearchGrid.Cursor cursor = WarDayBiomeSearchGrid.cursor(-125, 124, -125, 124);
        Set<String> exact = new HashSet<>();
        int prechecks = 0;
        while (cursor.hasNext()) {
            WarDayBiomeSearchGrid.Sample sample = cursor.next();
            if (sample.precheck()) {
                prechecks++;
            } else {
                exact.add(sample.blockX() + ":" + sample.blockZ());
            }
        }

        require(prechecks == 8, "expected eight fast boundary prechecks");
        require(exact.size() == 64 * 64, "250x250 arena must retain exhaustive quart coverage");
        require(exact.contains("-126:-126"), "minimum quart center missing");
        require(exact.contains("126:126"), "maximum quart center missing");
    }

    private static void verifiesNegativeCoordinateCoverage() {
        WarDayBiomeSearchGrid.Cursor cursor = WarDayBiomeSearchGrid.cursor(-381, -132, 67, 316);
        Set<String> exact = new HashSet<>();
        while (cursor.hasNext()) {
            WarDayBiomeSearchGrid.Sample sample = cursor.next();
            if (!sample.precheck()) {
                exact.add(sample.blockX() + ":" + sample.blockZ());
            }
        }
        require(exact.size() == 64 * 64, "negative source coordinates must retain exhaustive coverage");
        require(exact.contains("-382:66"), "negative minimum must use floor division");
    }

    private static void verifiesCursorCanResumeAcrossSmallBatches() {
        WarDayBiomeSearchGrid.Cursor cursor = WarDayBiomeSearchGrid.cursor(8, 257, 8, 257);
        int count = 0;
        while (cursor.hasNext()) {
            for (int batch = 0; batch < 7 && cursor.hasNext(); batch++) {
                cursor.next();
                count++;
            }
        }
        require(count == 8 + 63 * 63, "batched cursor must neither skip nor repeat exact work");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
