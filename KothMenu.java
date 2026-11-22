package com.ejemplo.koth;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class KothMenu implements InventoryHolder {
    public enum Type { MAIN, REWARDS_VIEW, REWARDS_EDIT }

    private final Type type;
    private Inventory inventory;

    public KothMenu(Type type) { this.type = type; }
    public Type type() { return type; }
    public void setInventory(Inventory inv) { this.inventory = inv; }

    @Override
    public Inventory getInventory() { return inventory; }
}
