package com.trove.warday;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class WarDaySpectatorCycleTest {
    private static final UUID ALPHA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BRAVO = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CHARLIE = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID MISSING = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private WarDaySpectatorCycleTest() {
    }

    public static void main(String[] args) {
        List<UUID> candidates = List.of(ALPHA, BRAVO, CHARLIE);

        expect(Optional.empty(), WarDaySpectatorCycle.select(List.of(), null, 1), "empty roster");
        expect(Optional.of(ALPHA), WarDaySpectatorCycle.select(candidates, null, 1), "initial forward");
        expect(Optional.of(CHARLIE), WarDaySpectatorCycle.select(candidates, null, -1), "initial backward");
        expect(Optional.of(BRAVO), WarDaySpectatorCycle.select(candidates, ALPHA, 1), "forward");
        expect(Optional.of(ALPHA), WarDaySpectatorCycle.select(candidates, BRAVO, -1), "backward");
        expect(Optional.of(ALPHA), WarDaySpectatorCycle.select(candidates, CHARLIE, 1), "forward wrap");
        expect(Optional.of(CHARLIE), WarDaySpectatorCycle.select(candidates, ALPHA, -1), "backward wrap");
        expect(Optional.of(ALPHA), WarDaySpectatorCycle.select(candidates, MISSING, 1), "missing forward");
        expect(Optional.of(CHARLIE), WarDaySpectatorCycle.select(candidates, MISSING, -1), "missing backward");
        expect(Optional.of(ALPHA), WarDaySpectatorCycle.select(List.of(ALPHA), ALPHA, 1), "single forward");
        expect(Optional.of(ALPHA), WarDaySpectatorCycle.select(List.of(ALPHA), ALPHA, -1), "single backward");
    }

    private static void expect(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }
}
