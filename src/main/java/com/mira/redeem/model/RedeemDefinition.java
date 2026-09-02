package com.mira.redeem.model;

import org.bukkit.Material;

import java.util.List;

public record RedeemDefinition(
        String id,
        Material material,
        String name,
        List<String> lore,
        List<String> commands,
        String successMessage
) {
}
