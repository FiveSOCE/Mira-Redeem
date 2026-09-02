package com.mira.redeem.service;

import com.mira.redeem.MiraRedeemPlugin;
import com.mira.redeem.model.RedeemDefinition;
import com.mira.redeem.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class RedeemService {
    private final MiraRedeemPlugin plugin;
    private final NamespacedKey redeemIdKey;
    private final NamespacedKey signatureKey;
    private final Map<String, RedeemDefinition> definitions = new LinkedHashMap<>();
    private byte[] signingSecret;

    public RedeemService(MiraRedeemPlugin plugin) {
        this.plugin = plugin;
        this.redeemIdKey = new NamespacedKey(plugin, "redeem_id");
        this.signatureKey = new NamespacedKey(plugin, "signature");
    }

    public void reload() {
        plugin.reloadConfig();
        ensureSigningSecret();
        definitions.clear();

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("redeems");
        if (root == null) return;

        for (String rawId : root.getKeys(false)) {
            String id = normalize(rawId);
            ConfigurationSection section = root.getConfigurationSection(rawId);
            if (section == null) continue;

            Material material = Material.matchMaterial(section.getString("material", "PAPER"));
            if (material == null) {
                plugin.getLogger().warning("Skipping redeem '" + rawId + "': invalid material.");
                continue;
            }

            String name = section.getString("name", "&fRedeem Voucher");
            List<String> lore = new ArrayList<>(section.getStringList("lore"));
            List<String> commands = new ArrayList<>(section.getStringList("commands"));
            String successMessage = section.getString("success-message", "&aRedeemed successfully.");

            if (commands.isEmpty()) {
                plugin.getLogger().warning("Skipping redeem '" + rawId + "': no commands configured.");
                continue;
            }

            definitions.put(id, new RedeemDefinition(id, material, name, lore, commands, successMessage));
        }

        plugin.getLogger().info("Loaded " + definitions.size() + " redeem definition(s).");
    }

    private void ensureSigningSecret() {
        String configured = plugin.getConfig().getString("security.signing-secret", "").trim();
        if (configured.isEmpty()) {
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            configured = Base64.getEncoder().encodeToString(generated);
            plugin.getConfig().set("security.signing-secret", configured);
            plugin.saveConfig();
            plugin.getLogger().info("Generated MiraRedeem signing secret.");
        }

        try {
            signingSecret = Base64.getDecoder().decode(configured);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("security.signing-secret is not valid Base64", ex);
        }
    }

    public Optional<RedeemDefinition> definition(String id) {
        return Optional.ofNullable(definitions.get(normalize(id)));
    }

    public List<String> ids() {
        return Collections.unmodifiableList(new ArrayList<>(definitions.keySet()));
    }

    public ItemStack createItem(String id, int amount) {
        RedeemDefinition definition = definitions.get(normalize(id));
        if (definition == null) throw new IllegalArgumentException("Unknown redeem: " + id);

        ItemStack item = new ItemStack(definition.material(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.component(definition.name()));
        meta.lore(definition.lore().stream().map(TextUtil::component).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(redeemIdKey, PersistentDataType.STRING, definition.id());
        meta.getPersistentDataContainer().set(signatureKey, PersistentDataType.STRING, signatureFor(definition.id()));
        item.setItemMeta(meta);
        return item;
    }

    public boolean isTagged(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(redeemIdKey, PersistentDataType.STRING);
    }

    public boolean isAuthentic(ItemStack item) {
        if (!isTagged(item)) return false;

        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(redeemIdKey, PersistentDataType.STRING);
        String signature = meta.getPersistentDataContainer().get(signatureKey, PersistentDataType.STRING);
        if (id == null || signature == null) return false;

        RedeemDefinition definition = definitions.get(normalize(id));
        if (definition == null) return false;

        byte[] supplied = signature.getBytes(StandardCharsets.UTF_8);
        byte[] expected = signatureFor(definition.id()).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(supplied, expected)) return false;

        ItemStack canonical = createItem(definition.id(), item.getAmount());
        return item.isSimilar(canonical);
    }

    public String redeemId(ItemStack item) {
        if (!isTagged(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(redeemIdKey, PersistentDataType.STRING);
    }

    public boolean execute(Player player, RedeemDefinition definition) {
        for (String configured : definition.commands()) {
            String command = configured
                    .replace("%player%", player.getName())
                    .replace("%username%", player.getName())
                    .replace("%uuid%", player.getUniqueId().toString())
                    .trim();
            if (command.startsWith("/")) command = command.substring(1);
            if (command.isBlank()) continue;

            boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            if (!dispatched) {
                plugin.getLogger().warning("Redeem '" + definition.id() + "' command failed to dispatch for " + player.getName() + ": " + command);
                return false;
            }
        }
        return true;
    }

    public String message(String path) {
        return plugin.getConfig().getString("messages." + path, "");
    }

    private String signatureFor(String id) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret, "HmacSHA256"));
            byte[] digest = mac.doFinal(normalize(id).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign redeem item", ex);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
