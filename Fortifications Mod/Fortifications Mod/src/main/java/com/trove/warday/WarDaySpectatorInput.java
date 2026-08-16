package com.trove.warday;

import java.util.OptionalInt;

final class WarDaySpectatorInput {
    private static final int LEFT_MOUSE_BUTTON = 0;
    private static final int RIGHT_MOUSE_BUTTON = 1;
    private static final int PRESS_ACTION = 1;

    private WarDaySpectatorInput() {
    }

    static OptionalInt cycleDirection(int button, int action) {
        if (action != PRESS_ACTION) {
            return OptionalInt.empty();
        }
        if (button == LEFT_MOUSE_BUTTON) {
            return OptionalInt.of(-1);
        }
        if (button == RIGHT_MOUSE_BUTTON) {
            return OptionalInt.of(1);
        }
        return OptionalInt.empty();
    }
}
