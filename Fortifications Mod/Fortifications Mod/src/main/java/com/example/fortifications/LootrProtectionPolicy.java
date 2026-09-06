package com.example.fortifications;

import java.util.Set;

public final class LootrProtectionPolicy {
    private static final String LOOTR_NAMESPACE = "lootr";
    private static final Set<String> PROTECTED_CONTAINER_PATHS = Set.of(
            "lootr_chest",
            "lootr_trapped_chest",
            "lootr_barrel",
            "lootr_inventory",
            "lootr_shulker",
            "decorated_pot"
    );

    private LootrProtectionPolicy() {
    }

    public static boolean isProtectedContainer(String namespace, String path) {
        return LOOTR_NAMESPACE.equals(namespace) && PROTECTED_CONTAINER_PATHS.contains(path);
    }

    public static boolean mayPlayerBreak(boolean creative, boolean sneaking, boolean fakePlayer) {
        return creative && sneaking && !fakePlayer;
    }
}
