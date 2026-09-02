package com.mira.redeem;

import com.mira.redeem.command.MiraRedeemCommand;
import com.mira.redeem.listener.ProtectionListener;
import com.mira.redeem.listener.RedeemListener;
import com.mira.redeem.service.RedeemService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiraRedeemPlugin extends JavaPlugin {
    private RedeemService redeemService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        redeemService = new RedeemService(this);
        redeemService.reload();

        getServer().getPluginManager().registerEvents(new RedeemListener(this, redeemService), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(redeemService), this);

        PluginCommand command = getCommand("miraredeem");
        if (command == null) {
            throw new IllegalStateException("miraredeem command missing from plugin.yml");
        }

        MiraRedeemCommand executor = new MiraRedeemCommand(redeemService);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        getLogger().info("MiraRedeem enabled.");
    }

    public RedeemService redeemService() {
        return redeemService;
    }
}
