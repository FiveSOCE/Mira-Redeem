package com.mira.redeem.command;

import com.mira.redeem.service.RedeemService;
import com.mira.redeem.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MiraRedeemCommand implements CommandExecutor, TabCompleter {
    private final RedeemService service;

    public MiraRedeemCommand(RedeemService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("miraredeem.admin")) {
            sender.sendMessage(TextUtil.component(service.message("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(TextUtil.component("&6MiraRedeem &7- /" + label + " <give|list|reload>"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> handleGive(sender, args);
            case "list" -> sender.sendMessage(TextUtil.component("&6Redeems: &f" + String.join(", ", service.ids())));
            case "reload" -> {
                service.reload();
                sender.sendMessage(TextUtil.component(service.message("reloaded")));
            }
            default -> sender.sendMessage(TextUtil.component("&cUsage: /" + label + " <give|list|reload>"));
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(TextUtil.component("&cUsage: /miraredeem give <player> <redeem> [amount]"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(TextUtil.component(service.message("player-not-found").replace("%player%", args[1])));
            return;
        }

        String id = args[2].toLowerCase(Locale.ROOT);
        if (service.definition(id).isEmpty()) {
            sender.sendMessage(TextUtil.component(service.message("unknown-redeem").replace("%redeem%", id)));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException ex) {
                sender.sendMessage(TextUtil.component("&cAmount must be a number from 1 to 64."));
                return;
            }
        }

        ItemStack item = service.createItem(id, amount);
        var leftovers = target.getInventory().addItem(item);
        leftovers.values().forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));

        sender.sendMessage(TextUtil.component(service.message("given")
                .replace("%amount%", String.valueOf(amount))
                .replace("%redeem%", id)
                .replace("%player%", target.getName())));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("miraredeem.admin")) return List.of();

        if (args.length == 1) return filter(List.of("give", "list", "reload"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) return filter(service.ids(), args[2]);
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) return filter(List.of("1", "2", "4", "8", "16", "32", "64"), args[3]);
        return List.of();
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(value);
        }
        return result;
    }
}
