package com.ejemplo.koth;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Koth {
    public final String name;
    public final World world;
    public final int minX, minY, minZ, maxX, maxY, maxZ;
    public final int captureSeconds;

    public Koth(String name, World world, Location a, Location b, int captureSeconds) {
        this.name = name;
        this.world = world;
        this.minX = Math.min(a.getBlockX(), b.getBlockX());
        this.minY = Math.min(a.getBlockY(), b.getBlockY());
        this.minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        this.maxX = Math.max(a.getBlockX(), b.getBlockX());
        this.maxY = Math.max(a.getBlockY(), b.getBlockY());
        this.maxZ = Math.max(a.getBlockZ(), b.getBlockZ());
        this.captureSeconds = captureSeconds;
    }

    public boolean contains(Player p) {
        Location l = p.getLocation();
        return l.getWorld().equals(world)
                && l.getBlockX() >= minX && l.getBlockX() <= maxX
                && l.getBlockY() >= minY && l.getBlockY() <= maxY
                && l.getBlockZ() >= minZ && l.getBlockZ() <= maxZ;
    }
}
