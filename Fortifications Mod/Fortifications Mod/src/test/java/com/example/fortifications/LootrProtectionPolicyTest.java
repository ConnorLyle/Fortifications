package com.example.fortifications;

public final class LootrProtectionPolicyTest {
    private LootrProtectionPolicyTest() {
    }

    public static void main(String[] args) {
        require(LootrProtectionPolicy.isProtectedContainer("lootr", "lootr_chest"), "Lootr chests must be protected");
        require(LootrProtectionPolicy.isProtectedContainer("lootr", "lootr_trapped_chest"), "trapped chests must be protected");
        require(LootrProtectionPolicy.isProtectedContainer("lootr", "lootr_barrel"), "barrels must be protected");
        require(LootrProtectionPolicy.isProtectedContainer("lootr", "lootr_inventory"), "converted inventories must be protected");
        require(LootrProtectionPolicy.isProtectedContainer("lootr", "lootr_shulker"), "shulkers must be protected");
        require(LootrProtectionPolicy.isProtectedContainer("lootr", "decorated_pot"), "decorated pots must be protected");
        require(!LootrProtectionPolicy.isProtectedContainer("lootr", "suspicious_sand"), "brushables must remain breakable");
        require(!LootrProtectionPolicy.isProtectedContainer("lootr", "trophy"), "trophies must remain breakable");
        require(!LootrProtectionPolicy.isProtectedContainer("minecraft", "chest"), "vanilla chests must remain breakable");

        require(!LootrProtectionPolicy.mayPlayerBreak(false, false, false), "Survival players must be blocked");
        require(!LootrProtectionPolicy.mayPlayerBreak(true, false, false), "Creative players must sneak");
        require(LootrProtectionPolicy.mayPlayerBreak(true, true, false), "sneaking Creative admins must be allowed");
        require(!LootrProtectionPolicy.mayPlayerBreak(true, true, true), "fake players must remain blocked");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
