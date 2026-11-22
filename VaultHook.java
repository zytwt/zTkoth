package com.ejemplo.koth;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {
    private Economy eco;

    public VaultHook() {
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) eco = rsp.getProvider();
    }

    public boolean isEnabled() { return eco != null; }

    public void give(Player p, double amount) {
        if (eco != null) eco.depositPlayer(p, amount);
    }
}
