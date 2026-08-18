package com.example.fortifications;

final class TeamChatPrefixPolicy {
    private TeamChatPrefixPolicy() {
    }

    static boolean shouldShowPrefix(boolean managerLoaded, boolean teamPresent, boolean personalTeam, String teamName) {
        return managerLoaded && teamPresent && !personalTeam && teamName != null && !teamName.isBlank();
    }
}
