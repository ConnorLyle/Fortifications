package com.example.fortifications;

final class ClassResetPolicy {
    private ClassResetPolicy() {
    }

    static boolean blocksForWarDay(boolean matchActive, boolean awaitingInventoryRestoration) {
        return matchActive || awaitingInventoryRestoration;
    }

    static boolean shouldSuppressStarterKit(
            boolean suppressionPending,
            String categoryNamespace,
            String categoryPath
    ) {
        return suppressionPending
                && "fortifications_classes".equals(categoryNamespace)
                && "example".equals(categoryPath);
    }
}
