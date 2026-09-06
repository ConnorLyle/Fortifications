package com.trove.warday;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class WarDayLegalitySearch {
    public static final int MAX_SAFE_DROP = 3;

    private static final int[][] CARDINAL_DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    private WarDayLegalitySearch() {
    }

    public static List<CornerTarget> cornerTargets(int minX, int minZ, int maxX, int maxZ) {
        return List.of(
                new CornerTarget("Northwest", minX, minZ),
                new CornerTarget("Northeast", maxX, minZ),
                new CornerTarget("Southwest", minX, maxZ),
                new CornerTarget("Southeast", maxX, maxZ)
        );
    }

    public interface Space {
        boolean isInside(int x, int z);

        boolean isStandable(Point point);

        boolean isGoal(Point point);
    }

    public static final class Cursor {
        private final Space space;
        private final int nodeLimit;
        private final Queue<Point> pending = new ArrayDeque<>();
        private final Set<Point> visited = new HashSet<>();
        private Status status = Status.RUNNING;
        private Point reached;

        public Cursor(Point start, Space space, int nodeLimit) {
            if (nodeLimit < 1) {
                throw new IllegalArgumentException("nodeLimit must be positive");
            }
            this.space = space;
            this.nodeLimit = nodeLimit;
            if (!space.isInside(start.x(), start.z()) || !space.isStandable(start)) {
                status = Status.NO_PATH;
                return;
            }
            pending.add(start);
            visited.add(start);
        }

        public boolean advance() {
            if (status != Status.RUNNING) {
                return false;
            }
            Point current = pending.poll();
            if (current == null) {
                status = Status.NO_PATH;
                return false;
            }
            if (space.isGoal(current)) {
                reached = current;
                status = Status.REACHED;
                return false;
            }

            for (Point neighbor : neighbors(current, space)) {
                if (visited.contains(neighbor)) {
                    continue;
                }
                if (visited.size() >= nodeLimit) {
                    status = Status.NODE_LIMIT;
                    return false;
                }
                visited.add(neighbor);
                pending.add(neighbor);
            }
            if (pending.isEmpty()) {
                status = Status.NO_PATH;
                return false;
            }
            return true;
        }

        public Status status() {
            return status;
        }

        public int visitedCount() {
            return visited.size();
        }

        public Point reached() {
            return reached;
        }
    }

    static List<Point> neighbors(Point from, Space space) {
        List<Point> result = new ArrayList<>(CARDINAL_DIRECTIONS.length);
        for (int[] direction : CARDINAL_DIRECTIONS) {
            int x = from.x() + direction[0];
            int z = from.z() + direction[1];
            if (!space.isInside(x, z)) {
                continue;
            }

            Point sameLevel = new Point(x, from.y(), z);
            if (space.isStandable(sameLevel)) {
                result.add(sameLevel);
                continue;
            }

            Point stepUp = new Point(x, from.y() + 1, z);
            if (space.isStandable(stepUp)) {
                result.add(stepUp);
                continue;
            }

            for (int drop = 1; drop <= MAX_SAFE_DROP; drop++) {
                Point stepDown = new Point(x, from.y() - drop, z);
                if (space.isStandable(stepDown)) {
                    result.add(stepDown);
                    break;
                }
            }
        }
        return result;
    }

    public enum Status {
        RUNNING,
        REACHED,
        NO_PATH,
        NODE_LIMIT
    }

    public record Point(int x, int y, int z) {
    }

    public record CornerTarget(String name, int x, int z) {
    }
}
