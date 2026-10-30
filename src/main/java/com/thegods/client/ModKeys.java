package com.thegods.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeys {
    public static final String CATEGORY = "key.categories.thegods";

    public static final KeyMapping MANIFEST = key("manifest", GLFW.GLFW_KEY_G);
    public static final KeyMapping USE_POWER = key("use_power", GLFW.GLFW_KEY_R);
    public static final KeyMapping NEXT_POWER = key("next_power", GLFW.GLFW_KEY_V);
    public static final KeyMapping PREV_POWER = key("prev_power", GLFW.GLFW_KEY_B);

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.thegods." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    private ModKeys() {}
}
