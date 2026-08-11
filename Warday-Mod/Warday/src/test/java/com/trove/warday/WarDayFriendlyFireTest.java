package com.trove.warday;

import java.util.Set;
import java.util.UUID;

public final class WarDayFriendlyFireTest {
    private WarDayFriendlyFireTest() {
    }

    public static void main(String[] args) {
        UUID defenderOne = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID defenderTwo = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID attackerOne = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID attackerTwo = UUID.fromString("00000000-0000-0000-0000-000000000004");
        UUID outsider = UUID.fromString("00000000-0000-0000-0000-000000000005");
        Set<UUID> defenders = Set.of(defenderOne, defenderTwo);
        Set<UUID> attackers = Set.of(attackerOne, attackerTwo);

        expect(true, WarDayFriendlyFire.areTeammates(defenderOne, defenderTwo, defenders, attackers), "defenders");
        expect(true, WarDayFriendlyFire.areTeammates(attackerOne, attackerTwo, defenders, attackers), "attackers");
        expect(false, WarDayFriendlyFire.areTeammates(defenderOne, attackerOne, defenders, attackers), "opponents");
        expect(false, WarDayFriendlyFire.areTeammates(defenderOne, outsider, defenders, attackers), "outsider");
    }

    private static void expect(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }
}
