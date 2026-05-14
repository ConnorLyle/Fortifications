package com.example.equalbreak.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    /** Default key: J — change in Controls settings any time. */
    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.equalbreak.toggle",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.equalbreak"
    );
}
