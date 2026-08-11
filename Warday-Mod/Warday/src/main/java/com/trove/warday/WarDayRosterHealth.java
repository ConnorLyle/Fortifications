package com.trove.warday;

final class WarDayRosterHealth {
    private WarDayRosterHealth() {
    }

    static Snapshot online(float health, float maxHealth) {
        int maxHealthPoints = roundedFiniteHealth(maxHealth, 1);
        int healthPoints = Math.clamp(roundedFiniteHealth(health, 0), 0, maxHealthPoints);
        return new Snapshot(Status.ONLINE, healthPoints, maxHealthPoints, 0);
    }

    static Snapshot respawning(int ticksRemaining) {
        int safeTicks = Math.max(1, ticksRemaining);
        int secondsRemaining = (int) Math.max(1L, ((long) safeTicks + 19L) / 20L);
        return new Snapshot(Status.RESPAWNING, 0, 0, secondsRemaining);
    }

    static Snapshot offline() {
        return new Snapshot(Status.OFFLINE, 0, 0, 0);
    }

    static String displayText(Snapshot snapshot) {
        return switch (snapshot.status()) {
            case ONLINE -> snapshot.healthPoints() + "/" + snapshot.maxHealthPoints();
            case RESPAWNING -> "RESP " + snapshot.respawnSeconds() + "s";
            case OFFLINE -> "OFF";
        };
    }

    static Band band(Snapshot snapshot) {
        if (snapshot.status() == Status.RESPAWNING) {
            return Band.RESPAWNING;
        }
        if (snapshot.status() == Status.OFFLINE) {
            return Band.OFFLINE;
        }
        if (snapshot.healthPoints() <= 0) {
            return Band.EMPTY;
        }

        double fraction = (double) snapshot.healthPoints() / snapshot.maxHealthPoints();
        if (fraction <= 0.25D) {
            return Band.CRITICAL;
        }
        if (fraction <= 0.5D) {
            return Band.LOW;
        }
        if (fraction <= 0.75D) {
            return Band.HURT;
        }
        return Band.HEALTHY;
    }

    private static int roundedFiniteHealth(float value, int fallback) {
        if (!Float.isFinite(value)) {
            return fallback;
        }
        return Math.max(fallback, (int) Math.ceil(value));
    }

    enum Status {
        ONLINE,
        RESPAWNING,
        OFFLINE
    }

    enum Band {
        HEALTHY,
        HURT,
        LOW,
        CRITICAL,
        EMPTY,
        RESPAWNING,
        OFFLINE
    }

    record Snapshot(Status status, int healthPoints, int maxHealthPoints, int respawnSeconds) {
    }
}
