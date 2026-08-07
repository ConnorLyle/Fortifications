package com.trove.warday;

import java.util.Optional;
import java.util.UUID;

public final class WarDayTeamVisibilityTest {
    private static final UUID RED = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BLUE = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private WarDayTeamVisibilityTest() {
    }

    public static void main(String[] args) {
        expect(true, WarDayTeamVisibility.journeyMapVisible(true, Optional.of(RED), Optional.of(RED)), "same team");
        expect(false, WarDayTeamVisibility.journeyMapVisible(true, Optional.of(RED), Optional.of(BLUE)), "different teams");
        expect(false, WarDayTeamVisibility.journeyMapVisible(false, Optional.of(RED), Optional.of(RED)), "preserve JourneyMap restriction");
        expect(false, WarDayTeamVisibility.journeyMapVisible(true, Optional.empty(), Optional.of(RED)), "missing receiver team");
        expect(false, WarDayTeamVisibility.journeyMapVisible(true, Optional.of(RED), Optional.empty()), "missing remote team");
        expect(false, WarDayTeamVisibility.journeyMapVisible(true, Optional.empty(), Optional.empty()), "teams unavailable");
    }

    private static void expect(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }
}
