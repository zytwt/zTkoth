package com.ejemplo.koth;

import org.bukkit.plugin.java.JavaPlugin;

public class KothPlugin extends JavaPlugin {
    private KothManager manager;
    private KothGui gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        RewardManager rewards = new RewardManager(this);
        manager = new KothManager(this, rewards);
        KothScheduler scheduler = new KothScheduler(this, manager);
        gui = new KothGui(manager, rewards, scheduler);

        getServer().getPluginManager().registerEvents(gui, this);
        getServer().getPluginManager().registerEvents(manager, this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new KothExpansion(this, manager, scheduler).register();
            getLogger().info("Placeholders de PlaceholderAPI registrados (%koth_...%).");
        }

        KothCommand cmd = new KothCommand(this, manager, scheduler, gui);
        getCommand("koth").setExecutor(cmd);
        getCommand("koth").setTabCompleter(cmd);
        getLogger().info("KothPlugin activado.");
    }

    @Override
    public void onDisable() {
        if (gui != null) gui.closeAll(); // guarda recompensas si alguien editaba
        if (manager != null) manager.stop(false);
    }
}
