package com.ejemplo.koth;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Guarda los items de recompensa en rewards.yml (editables desde la GUI). */
public class RewardManager {
    private final KothPlugin plugin;
    private final File file;
    private final List<ItemStack> items = new ArrayList<>();

    public RewardManager(KothPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "rewards.yml");
        load();
    }

    public void load() {
        items.clear();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (Object o : cfg.getList("items", new ArrayList<>())) {
            if (o instanceof ItemStack it && it.getType() != Material.AIR) items.add(it);
        }
    }

    public void save(Collection<ItemStack> newItems) {
        items.clear();
        for (ItemStack it : newItems) {
            if (it != null && it.getType() != Material.AIR) items.add(it.clone());
        }
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("items", items);
        try {
            plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar rewards.yml: " + e.getMessage());
        }
    }

    public List<ItemStack> getItems() {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack it : items) copy.add(it.clone());
        return copy;
    }

    public void give(Player p) {
        for (ItemStack it : getItems()) {
            Map<Integer, ItemStack> left = p.getInventory().addItem(it);
            // Si no cabe, se tira al suelo
            left.values().forEach(l -> p.getWorld().dropItemNaturally(p.getLocation(), l));
        }
    }
}
