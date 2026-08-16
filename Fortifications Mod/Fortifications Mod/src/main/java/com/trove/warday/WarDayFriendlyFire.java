package com.trove.warday;

import java.util.Set;
import java.util.UUID;

final class WarDayFriendlyFire {
    private WarDayFriendlyFire() {
    }

    static boolean areTeammates(UUID attacker, UUID victim, Set<UUID> defenders, Set<UUID> attackers) {
        return defenders.contains(attacker) && defenders.contains(victim)
                || attackers.contains(attacker) && attackers.contains(victim);
    }
}
