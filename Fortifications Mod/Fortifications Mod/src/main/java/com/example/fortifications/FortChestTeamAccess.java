package com.example.fortifications;

import java.util.UUID;

final class FortChestTeamAccess {
    private FortChestTeamAccess() {
    }

    static boolean canAccess(UUID ownerTeamId, UUID playerTeamId) {
        return ownerTeamId != null && ownerTeamId.equals(playerTeamId);
    }
}
