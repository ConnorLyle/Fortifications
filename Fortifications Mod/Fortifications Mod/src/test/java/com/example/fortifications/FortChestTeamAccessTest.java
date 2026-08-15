package com.example.fortifications;

import java.util.UUID;

public final class FortChestTeamAccessTest {
    private FortChestTeamAccessTest() {
    }

    public static void main(String[] args) {
        UUID red = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID blue = UUID.fromString("00000000-0000-0000-0000-000000000002");
        require(FortChestTeamAccess.canAccess(red, red), "members of the bound team must have access");
        require(!FortChestTeamAccess.canAccess(red, blue), "other teams must remain isolated");
        require(!FortChestTeamAccess.canAccess(null, red), "unbound chests must not expose inventory");
        require(!FortChestTeamAccess.canAccess(red, null), "players without a resolved team must not have access");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
