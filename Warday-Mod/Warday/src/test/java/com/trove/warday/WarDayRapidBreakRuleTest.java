package com.trove.warday;

import java.util.ArrayDeque;
import java.util.Deque;

public final class WarDayRapidBreakRuleTest {
    private WarDayRapidBreakRuleTest() {
    }

    public static void main(String[] args) {
        Deque<Long> breaks = new ArrayDeque<>();
        for (int i = 0; i < 14; i++) {
            expect(false, WarDayRapidBreakRule.recordBreak(breaks, i * 20L, 600, 15).triggered(), "before threshold " + i);
        }
        expect(true, WarDayRapidBreakRule.recordBreak(breaks, 280L, 600, 15).triggered(), "fifteenth break triggers");
        expect(0, breaks.size(), "history resets after trigger");

        for (int i = 0; i < 15; i++) {
            WarDayRapidBreakRule.TrackResult result = WarDayRapidBreakRule.recordBreak(breaks, 1_000L + i, 600, 15);
            expect(i == 14, result.triggered(), "retrigger cycle " + i);
        }

        breaks.addLast(0L);
        WarDayRapidBreakRule.prune(breaks, 600L, 600);
        expect(1, breaks.size(), "exact window boundary retained");
        WarDayRapidBreakRule.prune(breaks, 601L, 600);
        expect(0, breaks.size(), "expired boundary removed");

        expect(-0.25D, WarDayRapidBreakRule.blockBreakSpeedModifier(25), "default penalty");
        expect(-1.0D, WarDayRapidBreakRule.blockBreakSpeedModifier(150), "upper clamp");
        expect(0.0D, WarDayRapidBreakRule.blockBreakSpeedModifier(-1), "lower clamp");
    }

    private static void expect(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }
}
