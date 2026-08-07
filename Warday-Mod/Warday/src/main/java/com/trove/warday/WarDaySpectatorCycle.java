package com.trove.warday;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class WarDaySpectatorCycle {
    private WarDaySpectatorCycle() {
    }

    static Optional<UUID> select(List<UUID> orderedCandidates, UUID currentTarget, int direction) {
        if (orderedCandidates.isEmpty()) {
            return Optional.empty();
        }

        int step = direction < 0 ? -1 : 1;
        int currentIndex = currentTarget == null ? -1 : orderedCandidates.indexOf(currentTarget);
        if (currentIndex < 0) {
            return Optional.of(step < 0
                    ? orderedCandidates.getLast()
                    : orderedCandidates.getFirst());
        }

        int nextIndex = Math.floorMod(currentIndex + step, orderedCandidates.size());
        return Optional.of(orderedCandidates.get(nextIndex));
    }
}
