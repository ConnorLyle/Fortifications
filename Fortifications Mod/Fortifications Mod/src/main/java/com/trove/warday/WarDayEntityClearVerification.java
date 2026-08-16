package com.trove.warday;

final class WarDayEntityClearVerification {
    static final int REQUIRED_EMPTY_PASSES = 2;

    private WarDayEntityClearVerification() {
    }

    static int nextEmptyPasses(int currentEmptyPasses, int entitiesDiscarded) {
        if (entitiesDiscarded < 0) {
            throw new IllegalArgumentException("entitiesDiscarded must not be negative");
        }
        return entitiesDiscarded == 0 ? currentEmptyPasses + 1 : 0;
    }

    static boolean isVerified(int consecutiveEmptyPasses) {
        return consecutiveEmptyPasses >= REQUIRED_EMPTY_PASSES;
    }
}
