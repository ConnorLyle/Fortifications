package com.example.fortifications;

public final class ClassResetPolicyTest {
    private ClassResetPolicyTest() {
    }

    public static void main(String[] args) {
        assertFalse(ClassResetPolicy.blocksForWarDay(false, false), "idle players may reset");
        assertTrue(ClassResetPolicy.blocksForWarDay(true, false), "active matches block resets");
        assertTrue(ClassResetPolicy.blocksForWarDay(false, true), "pending restoration blocks resets");

        assertFalse(ClassResetPolicy.shouldSuppressStarterKit(
                false, "fortifications_classes", "example"), "suppression must be pending");
        assertFalse(ClassResetPolicy.shouldSuppressStarterKit(
                true, "other", "example"), "other namespaces must not be suppressed");
        assertFalse(ClassResetPolicy.shouldSuppressStarterKit(
                true, "fortifications_classes", "other"), "other categories must not be suppressed");
        assertTrue(ClassResetPolicy.shouldSuppressStarterKit(
                true, "fortifications_classes", "example"), "the next class unlock is suppressed");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean value, String message) {
        assertTrue(!value, message);
    }
}
