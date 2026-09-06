package com.trove.warday;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WarDayLegalitySearchTest {
    private WarDayLegalitySearchTest() {
    }

    public static void main(String[] args) {
        var corners = WarDayLegalitySearch.cornerTargets(-32, -16, 15, 31);
        expect(new WarDayLegalitySearch.CornerTarget("Northwest", -32, -16), corners.get(0), "northwest");
        expect(new WarDayLegalitySearch.CornerTarget("Northeast", 15, -16), corners.get(1), "northeast");
        expect(new WarDayLegalitySearch.CornerTarget("Southwest", -32, 31), corners.get(2), "southwest");
        expect(new WarDayLegalitySearch.CornerTarget("Southeast", 15, 31), corners.get(3), "southeast");

        Set<WarDayLegalitySearch.Point> flat = new HashSet<>();
        for (int x = 0; x <= 4; x++) {
            flat.add(new WarDayLegalitySearch.Point(x, 1, 0));
        }
        expect(WarDayLegalitySearch.Status.REACHED,
                run(flat, new WarDayLegalitySearch.Point(0, 1, 0), new WarDayLegalitySearch.Point(4, 1, 0), 100),
                "flat route");

        Set<WarDayLegalitySearch.Point> blocked = new HashSet<>(flat);
        blocked.remove(new WarDayLegalitySearch.Point(2, 1, 0));
        expect(WarDayLegalitySearch.Status.NO_PATH,
                run(blocked, new WarDayLegalitySearch.Point(0, 1, 0), new WarDayLegalitySearch.Point(4, 1, 0), 100),
                "sealed corridor");

        Set<WarDayLegalitySearch.Point> oneStep = Set.of(
                new WarDayLegalitySearch.Point(0, 1, 0),
                new WarDayLegalitySearch.Point(1, 2, 0),
                new WarDayLegalitySearch.Point(2, 2, 0));
        expect(WarDayLegalitySearch.Status.REACHED,
                run(oneStep, new WarDayLegalitySearch.Point(0, 1, 0), new WarDayLegalitySearch.Point(2, 2, 0), 100),
                "one-block step");

        Set<WarDayLegalitySearch.Point> twoStep = Set.of(
                new WarDayLegalitySearch.Point(0, 1, 0),
                new WarDayLegalitySearch.Point(1, 3, 0));
        expect(WarDayLegalitySearch.Status.NO_PATH,
                run(twoStep, new WarDayLegalitySearch.Point(0, 1, 0), new WarDayLegalitySearch.Point(1, 3, 0), 100),
                "two-block wall");

        Set<WarDayLegalitySearch.Point> safeDrop = Set.of(
                new WarDayLegalitySearch.Point(0, 4, 0),
                new WarDayLegalitySearch.Point(1, 1, 0));
        expect(WarDayLegalitySearch.Status.REACHED,
                run(safeDrop, new WarDayLegalitySearch.Point(0, 4, 0), new WarDayLegalitySearch.Point(1, 1, 0), 100),
                "three-block drop");

        Set<WarDayLegalitySearch.Point> unsafeDrop = Set.of(
                new WarDayLegalitySearch.Point(0, 5, 0),
                new WarDayLegalitySearch.Point(1, 1, 0));
        expect(WarDayLegalitySearch.Status.NO_PATH,
                run(unsafeDrop, new WarDayLegalitySearch.Point(0, 5, 0), new WarDayLegalitySearch.Point(1, 1, 0), 100),
                "four-block drop");

        expect(WarDayLegalitySearch.Status.NODE_LIMIT,
                run(flat, new WarDayLegalitySearch.Point(0, 1, 0), new WarDayLegalitySearch.Point(4, 1, 0), 2),
                "node limit");
    }

    private static WarDayLegalitySearch.Status run(
            Set<WarDayLegalitySearch.Point> standable,
            WarDayLegalitySearch.Point start,
            WarDayLegalitySearch.Point goal,
            int nodeLimit
    ) {
        int minX = standable.stream().mapToInt(WarDayLegalitySearch.Point::x).min().orElse(0);
        int maxX = standable.stream().mapToInt(WarDayLegalitySearch.Point::x).max().orElse(0);
        int minZ = standable.stream().mapToInt(WarDayLegalitySearch.Point::z).min().orElse(0);
        int maxZ = standable.stream().mapToInt(WarDayLegalitySearch.Point::z).max().orElse(0);
        WarDayLegalitySearch.Space space = new WarDayLegalitySearch.Space() {
            @Override
            public boolean isInside(int x, int z) {
                return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
            }

            @Override
            public boolean isStandable(WarDayLegalitySearch.Point point) {
                return standable.contains(point);
            }

            @Override
            public boolean isGoal(WarDayLegalitySearch.Point point) {
                return goal.equals(point);
            }
        };
        WarDayLegalitySearch.Cursor cursor = new WarDayLegalitySearch.Cursor(start, space, nodeLimit);
        while (cursor.advance()) {
            // Advance to a terminal state.
        }
        return cursor.status();
    }

    private static void expect(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
