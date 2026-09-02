package com.mira.redeem.listener;

import com.mira.redeem.MiraRedeemPlugin;
import com.mira.redeem.model.RedeemDefinition;
import com.mira.redeem.service.RedeemService;
import com.mira.redeem.util.TextUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class RedeemListener implements Listener {
    private final MiraRedeemPlugin plugin;
    private final RedeemService service;
    private final Set<UUID> redeeming = new HashSet<>();

    public RedeemListener(MiraRedeemPlugin plugin, RedeemService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() == null) return;

        ItemStack item = event.getItem();
        if (!service.isTagged(item)) return;

        event.setCancelled(true);

        if (!service.isAuthentic(item)) {
            event.getPlayer().sendMessage(TextUtil.component(service.message("invalid-item")));
            return;
        }

        UUID uuid = event.getPlayer().getUniqueId();
        if (!redeeming.add(uuid)) return;

        try {
            String id = service.redeemId(item);
            RedeemDefinition definition = service.definition(id).orElse(null);
            if (definition == null) {
                event.getPlayer().sendMessage(TextUtil.component(service.message("invalid-item")));
                return;
            }

            if (!service.execute(event.getPlayer(), definition)) {
                event.getPlayer().sendMessage(TextUtil.component(service.message("command-failed")));
                return;
            }

            event.getPlayer().sendMessage(TextUtil.component(definition.successMessage()));
            consumeOne(event.getPlayer(), event.getHand());
        } finally {
            plugin.getServer().getScheduler().runTask(plugin, () -> redeeming.remove(uuid));
        }
    }

    private void consumeOne(org.bukkit.entity.Player player, EquipmentSlot hand) {
        ItemStack held = hand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        if (!service.isAuthentic(held)) return;

        if (held.getAmount() <= 1) {
            if (hand == EquipmentSlot.HAND) player.getInventory().setItemInMainHand(null);
            else player.getInventory().setItemInOffHand(null);
        } else {
            held.setAmount(held.getAmount() - 1);
        }
    }
}
