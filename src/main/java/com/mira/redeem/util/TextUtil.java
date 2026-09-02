package com.mira.redeem.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class TextUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private TextUtil() {
    }

    public static Component component(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }
}
