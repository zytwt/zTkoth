package com.ejemplo.koth;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * Placeholders de PlaceholderAPI con el identificador "koth".
 * Ejemplo: %koth_status%, %koth_next_in%, %koth_schedule_1%
 */
public class KothExpansion extends PlaceholderExpansion {
    private final KothPlugin plugin;
    private final KothManager manager;
    private final KothScheduler scheduler;

    public KothExpansion(KothPlugin plugin, KothManager manager, KothScheduler scheduler) {
        this.plugin = plugin;
        this.manager = manager;
        this.scheduler = scheduler;
    }

    @Override public String getIdentifier() { return "koth"; }
    @Override public String getAuthor() { return "KothPlugin"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    private String t(String key, String def) {
        return plugin.getConfig().getString("placeholders." + key, def);
    }

    private static String mmss(int secs) {
        return (secs / 60) + ":" + String.format("%02d", secs % 60);
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String p = params.toLowerCase();
        Koth act = manager.getActive();

        // %koth_schedule_N%  y  %koth_schedulein_N%
        if (p.startsWith("schedule_") || p.startsWith("schedulein_")) {
            try {
                int n = Integer.parseInt(p.substring(p.indexOf('_') + 1));
                return p.startsWith("schedulein_") ? scheduler.lineIn(n) : scheduler.line(n);
            } catch (NumberFormatException e) {
                return "";
            }
        }

        switch (p) {
            // --- Evento actual ---
            case "active":       return String.valueOf(act != null);
            case "status":       return act != null ? t("activo", "Activo") : t("inactivo", "Inactivo");
            case "name":         return act != null ? act.name : t("ninguno", "Ninguno");
            case "capturer": {
                String n = manager.getCapturerName();
                return n != null ? n : t("nadie", "Nadie");
            }
            case "contested":    return String.valueOf(manager.isContested());
            case "progress":     return String.valueOf(manager.getProgress());
            case "total":        return String.valueOf(manager.getTotalTime());
            case "time_left":    return mmss(manager.getTimeLeft());
            case "time_left_seconds": return String.valueOf(manager.getTimeLeft());
            case "percent": {
                int total = manager.getTotalTime();
                return total == 0 ? "0" : String.valueOf(Math.min(100, manager.getProgress() * 100 / total));
            }
            // --- Horarios ---
            case "next":         return scheduler.next();
            case "next_name":    return scheduler.nextName();
            case "next_time":    return scheduler.nextTime();
            case "next_in":      return scheduler.nextIn();
            case "schedule_count": return String.valueOf(scheduler.count());
            case "koth_count":   return String.valueOf(manager.all().size());
            default:             return null;
        }
    }
}
