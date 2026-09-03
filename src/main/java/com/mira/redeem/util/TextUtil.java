package com.mira.redeem.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class TextUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final String CHAT_PREFIX = "&5&lMira &8>> &r";

    private TextUtil() {
    }

    public static Component component(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }

    public static Component chat(String text) {
        return component(CHAT_PREFIX + (text == null ? "" : text));
    }
}
