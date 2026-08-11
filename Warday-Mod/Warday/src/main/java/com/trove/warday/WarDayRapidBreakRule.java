package com.trove.warday;

import java.util.Deque;

public final class WarDayRapidBreakRule {
    private WarDayRapidBreakRule() {
    }

    public static TrackResult recordBreak(Deque<Long> breaks, long gameTime, int windowTicks, int threshold) {
        prune(breaks, gameTime, windowTicks);
        breaks.addLast(gameTime);
        boolean triggered = breaks.size() >= Math.max(1, threshold);
        int recentBreaks = breaks.size();
        if (triggered) {
            breaks.clear();
        }
        return new TrackResult(triggered, recentBreaks);
    }

    public static void prune(Deque<Long> breaks, long gameTime, int windowTicks) {
        while (!breaks.isEmpty() && gameTime - breaks.peekFirst() > Math.max(0, windowTicks)) {
            breaks.removeFirst();
        }
    }

    public static double blockBreakSpeedModifier(int penaltyPercent) {
        int clampedPercent = Math.max(0, Math.min(100, penaltyPercent));
        return -clampedPercent / 100.0D;
    }

    public static int cumulativePenaltyPercent(int strike, int firstPenaltyPercent) {
        int safeStrike = Math.max(1, strike);
        int penalty = firstPenaltyPercent;
        if (safeStrike >= 2) {
            penalty += 20;
        }
        if (safeStrike >= 3) {
            penalty += 10;
        }
        if (safeStrike >= 4) {
            penalty += 5;
        }
        return Math.clamp(penalty, 0, 100);
    }

    public record TrackResult(boolean triggered, int recentBreaks) {
    }
}
