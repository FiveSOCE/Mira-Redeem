package com.mira.redeem.model;

import org.bukkit.Material;

import java.util.List;

public record RedeemDefinition(
        String id,
        RedeemType type,
        Material material,
        String name,
        List<String> lore,
        List<String> commands,
        String successMessage,
        String track,
        String targetGroup
) {
}
