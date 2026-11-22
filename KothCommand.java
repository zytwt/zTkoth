package com.ejemplo.koth;

import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class KothCommand implements CommandExecutor, TabCompleter {
    private final KothPlugin plugin;
    private final KothManager manager;
    private final KothScheduler scheduler;
    private final KothGui gui;
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    public KothCommand(KothPlugin plugin, KothManager manager, KothScheduler scheduler, KothGui gui) {
        this.plugin = plugin; this.manager = manager; this.scheduler = scheduler; this.gui = gui;
    }

    private Player player(CommandSender s) {
        if (s instanceof Player pl) return pl;
        s.sendMessage("Solo jugadores.");
        return null;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        String sub = a.length == 0 ? "gui" : a[0].toLowerCase();

        // ---- Abiertos a todos ----
        if (sub.equals("gui") || sub.equals("recompensas") || sub.equals("premios")) {
            Player pl = player(s);
            if (pl == null) return true;
            boolean admin = pl.hasPermission("koth.admin");
            if (sub.equals("gui") && admin) gui.openMain(pl);
            else if (a.length > 1 && a[1].equalsIgnoreCase("editar") && admin) gui.openRewards(pl, true);
            else gui.openRewards(pl, false);
            return true;
        }

        // ---- Solo admin ----
        if (!s.hasPermission("koth.admin")) { s.sendMessage("§cSin permiso."); return true; }

        switch (sub) {
            case "pos1", "pos2" -> {
                Player p = player(s);
                if (p == null) return true;
                (sub.equals("pos1") ? pos1 : pos2).put(p.getUniqueId(), p.getLocation());
                s.sendMessage("§a" + sub + " guardada.");
            }
            case "create" -> {
                Player p = player(s);
                if (p == null) return true;
                if (a.length < 2) { s.sendMessage("§e/koth create <nombre> [segundos]"); return true; }
                Location p1 = pos1.get(p.getUniqueId()), p2 = pos2.get(p.getUniqueId());
                if (p1 == null || p2 == null) { s.sendMessage("§cDefine pos1 y pos2 primero."); return true; }
                int secs = a.length > 2 ? parse(a[2], 300) : 300;
                manager.create(a[1], p1, p2, secs);
                s.sendMessage("§aKOTH '" + a[1] + "' creado (" + secs + "s). Abre /koth para gestionarlo.");
            }
            case "delete" -> {
                if (a.length < 2) return true;
                s.sendMessage(manager.delete(a[1]) ? "§aEliminado." : "§cNo existe.");
            }
            case "start" -> {
                if (a.length < 2) { s.sendMessage("§e/koth start <nombre>"); return true; }
                Koth k = manager.get(a[1]);
                if (k == null) s.sendMessage("§cNo existe.");
                else if (!manager.start(k)) s.sendMessage("§cYa hay un KOTH activo.");
            }
            case "stop" -> {
                if (!manager.isRunning()) s.sendMessage("§cNo hay KOTH activo.");
                else manager.stop(true);
            }
            case "list" -> s.sendMessage("§eKOTHs: §f" +
                    manager.all().stream().map(k -> k.name).collect(Collectors.joining(", ")));
            case "next" -> s.sendMessage("§eProximo: §f" + scheduler.next());
            case "reload" -> {
                plugin.reloadConfig();
                manager.load();
                s.sendMessage("§aConfig recargada.");
            }
            default -> s.sendMessage("§cSubcomando desconocido.");
        }
        return true;
    }

    private int parse(String s, int def) {
        try { return Math.max(1, Integer.parseInt(s)); } catch (NumberFormatException e) { return def; }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1)
            return List.of("gui", "recompensas", "pos1", "pos2", "create", "delete", "start", "stop", "list", "next", "reload");
        if (a.length == 2 && a[0].equalsIgnoreCase("recompensas"))
            return List.of("editar");
        if (a.length == 2 && List.of("start", "delete").contains(a[0].toLowerCase()))
            return manager.all().stream().map(k -> k.name).collect(Collectors.toList());
        return List.of();
    }
}
