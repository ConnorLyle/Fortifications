package com.example.fortifications;

public final class TeamChatPrefixPolicyTest {
    private TeamChatPrefixPolicyTest() {
    }

    public static void main(String[] args) {
        assertFalse(TeamChatPrefixPolicy.shouldShowPrefix(false, true, false, "Builders"),
                "an unavailable manager must not add a prefix");
        assertFalse(TeamChatPrefixPolicy.shouldShowPrefix(true, false, false, "Builders"),
                "a missing team must not add a prefix");
        assertFalse(TeamChatPrefixPolicy.shouldShowPrefix(true, true, true, "Player"),
                "automatic personal teams must not produce redundant prefixes");
        assertFalse(TeamChatPrefixPolicy.shouldShowPrefix(true, true, false, "  "),
                "blank team names must not add a prefix");
        assertTrue(TeamChatPrefixPolicy.shouldShowPrefix(true, true, false, "Builders"),
                "named party or server teams should add a prefix");
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
