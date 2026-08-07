package com.trove.warday;

public final class WarDayRosterHealthTest {
    private WarDayRosterHealthTest() {
    }

    public static void main(String[] args) {
        expectOnline(20.0F, 20.0F, "10/10\u2665", WarDayRosterHealth.Band.HEALTHY);
        expectOnline(19.0F, 20.0F, "9.5/10\u2665", WarDayRosterHealth.Band.HEALTHY);
        expectOnline(15.0F, 20.0F, "7.5/10\u2665", WarDayRosterHealth.Band.HURT);
        expectOnline(10.0F, 20.0F, "5/10\u2665", WarDayRosterHealth.Band.LOW);
        expectOnline(5.0F, 20.0F, "2.5/10\u2665", WarDayRosterHealth.Band.CRITICAL);
        expectOnline(0.0F, 20.0F, "0/10\u2665", WarDayRosterHealth.Band.EMPTY);
        expectOnline(-5.0F, 20.0F, "0/10\u2665", WarDayRosterHealth.Band.EMPTY);
        expectOnline(60.0F, 40.0F, "20/20\u2665", WarDayRosterHealth.Band.HEALTHY);
        expectOnline(Float.NaN, Float.NaN, "0/0.5\u2665", WarDayRosterHealth.Band.EMPTY);

        expectRespawning(1, "RESP 1s");
        expectRespawning(20, "RESP 1s");
        expectRespawning(21, "RESP 2s");
        expectRespawning(199, "RESP 10s");

        WarDayRosterHealth.Snapshot offline = WarDayRosterHealth.offline();
        expect("OFF", WarDayRosterHealth.displayText(offline), "offline text");
        expect(WarDayRosterHealth.Band.OFFLINE, WarDayRosterHealth.band(offline), "offline band");
    }

    private static void expectOnline(
            float health,
            float maxHealth,
            String expectedText,
            WarDayRosterHealth.Band expectedBand
    ) {
        WarDayRosterHealth.Snapshot snapshot = WarDayRosterHealth.online(health, maxHealth);
        expect(expectedText, WarDayRosterHealth.displayText(snapshot), "online text");
        expect(expectedBand, WarDayRosterHealth.band(snapshot), "online band");
    }

    private static void expectRespawning(int ticks, String expectedText) {
        WarDayRosterHealth.Snapshot snapshot = WarDayRosterHealth.respawning(ticks);
        expect(expectedText, WarDayRosterHealth.displayText(snapshot), "respawn text");
        expect(WarDayRosterHealth.Band.RESPAWNING, WarDayRosterHealth.band(snapshot), "respawn band");
    }

    private static void expect(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }
}
