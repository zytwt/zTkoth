package com.ejemplo.koth;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class KothManager implements Listener {
    private final KothPlugin plugin;
    private final RewardManager rewards;
    private final Map<String, Koth> koths = new HashMap<>();
    private VaultHook vault;

    // Estado del evento activo
    private Koth active;
    private UUID capturer;
    private UUID glowing;
    private boolean contested;
    private int progress;
    private BossBar bar;
    private KothScoreboard sb;
    private BukkitTask task, auraTask;
    private Particle particle = Particle.FLAME;
    private double auraAngle;

    public KothManager(KothPlugin plugin, RewardManager rewards) {
        this.plugin = plugin;
        this.rewards = rewards;
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vault = new VaultHook();
            if (!vault.isEnabled()) vault = null;
        }
        load();
    }

    public String msg(String key) {
        String p = plugin.getConfig().getString("mensajes.prefijo", "");
        return color(p + plugin.getConfig().getString("mensajes." + key, key));
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    // ---------- Persistencia ----------
    public void load() {
        koths.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("koths");
        if (sec == null) return;
        for (String name : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(name);
            World w = Bukkit.getWorld(s.getString("mundo", ""));
            if (w == null) continue;
            Location a = new Location(w, s.getInt("x1"), s.getInt("y1"), s.getInt("z1"));
            Location b = new Location(w, s.getInt("x2"), s.getInt("y2"), s.getInt("z2"));
            koths.put(name.toLowerCase(), new Koth(name, w, a, b, s.getInt("segundos", 300)));
        }
    }

    public void create(String name, Location a, Location b, int seconds) {
        String path = "koths." + name.toLowerCase();
        plugin.getConfig().set(path + ".mundo", a.getWorld().getName());
        plugin.getConfig().set(path + ".x1", a.getBlockX());
        plugin.getConfig().set(path + ".y1", a.getBlockY());
        plugin.getConfig().set(path + ".z1", a.getBlockZ());
        plugin.getConfig().set(path + ".x2", b.getBlockX());
        plugin.getConfig().set(path + ".y2", b.getBlockY());
        plugin.getConfig().set(path + ".z2", b.getBlockZ());
        plugin.getConfig().set(path + ".segundos", seconds);
        plugin.saveConfig();
        koths.put(name.toLowerCase(), new Koth(name, a.getWorld(), a, b, seconds));
    }

    public boolean delete(String name) {
        if (koths.remove(name.toLowerCase()) == null) return false;
        plugin.getConfig().set("koths." + name.toLowerCase(), null);
        plugin.saveConfig();
        return true;
    }

    public Collection<Koth> all() { return koths.values(); }
    public Koth get(String name) { return koths.get(name.toLowerCase()); }
    public Koth getActive() { return active; }
    public boolean isRunning() { return active != null; }
    public boolean isContested() { return active != null && contested; }
    public int getProgress() { return progress; }
    public int getTotalTime() { return active == null ? 0 : active.captureSeconds; }
    public int getTimeLeft() { return active == null ? 0 : Math.max(0, active.captureSeconds - progress); }
    public String getCapturerName() {
        if (capturer == null) return null;
        Player p = Bukkit.getPlayer(capturer);
        return p == null ? null : p.getName();
    }

    // ---------- Evento ----------
    public boolean start(Koth k) {
        if (active != null) return false;
        active = k;
        capturer = null;
        contested = false;
        progress = 0;
        bar = Bukkit.createBossBar(color("&6KOTH " + k.name), BarColor.YELLOW, BarStyle.SOLID);
        if (plugin.getConfig().getBoolean("scoreboard.activado", true)) {
            sb = new KothScoreboard(color(plugin.getConfig().getString("scoreboard.titulo", "&6KOTH")));
        }
        particle = resolveParticle();
        Bukkit.broadcastMessage(msg("inicio")
                .replace("%koth%", k.name).replace("%tiempo%", String.valueOf(k.captureSeconds)));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        auraTask = Bukkit.getScheduler().runTaskTimer(plugin, this::aura, 2L, 2L);
        tick();
        return true;
    }

    public void stop(boolean announce) {
        if (active == null) return;
        if (task != null) task.cancel();
        if (auraTask != null) auraTask.cancel();
        updateGlow(null);
        if (bar != null) bar.removeAll();
        if (sb != null) sb.hideAll();
        if (announce) Bukkit.broadcastMessage(msg("detenido").replace("%koth%", active.name));
        active = null; capturer = null; progress = 0; contested = false;
        bar = null; sb = null; task = null; auraTask = null;
    }

    private void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            bar.addPlayer(p);
            if (sb != null) sb.show(p);
        }

        List<Player> inside = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isDead() && active.contains(p)) inside.add(p);
        }

        String who;
        if (inside.size() > 1) {
            contested = true;
            who = "&cDisputa";
            bar.setTitle(color(plugin.getConfig().getString("mensajes.disputa", "Disputa")));
            bar.setColor(BarColor.RED);
            updateGlow(null);
        } else if (inside.isEmpty()) {
            contested = false;
            capturer = null; progress = 0;
            who = "&7Nadie";
            bar.setTitle(color("&6KOTH " + active.name + " &7- libre"));
            bar.setColor(BarColor.YELLOW);
            bar.setProgress(0);
            updateGlow(null);
        } else {
            contested = false;
            Player p = inside.get(0);
            if (!p.getUniqueId().equals(capturer)) { capturer = p.getUniqueId(); progress = 0; }
            progress++;
            who = "&f" + p.getName();
            updateGlow(p);
            bar.setColor(BarColor.GREEN);
            bar.setTitle(color("&6" + p.getName() + " &7capturando &e" + progress + "/" + active.captureSeconds));
            bar.setProgress(Math.min(1.0, (double) progress / active.captureSeconds));
            if (progress >= active.captureSeconds) { win(p); return; }
        }

        if (sb != null) {
            int left = Math.max(0, active.captureSeconds - progress);
            sb.update(List.of(
                    color("&eKOTH: &f" + active.name),
                    " ",
                    color("&eCapturando:"),
                    color(who),
                    "  ",
                    color("&eTiempo: &f" + (left / 60) + ":" + String.format("%02d", left % 60))
            ));
        }
    }

    // ---------- Aura ----------
    private Particle resolveParticle() {
        try {
            return Particle.valueOf(plugin.getConfig().getString("aura.particula", "FLAME").toUpperCase());
        } catch (IllegalArgumentException e) {
            return Particle.FLAME;
        }
    }

    private void aura() {
        if (!plugin.getConfig().getBoolean("aura.activada", true)) return;
        if (capturer == null || contested) return;
        Player p = Bukkit.getPlayer(capturer);
        if (p == null || !p.isOnline()) return;

        double r = plugin.getConfig().getDouble("aura.radio", 0.9);
        double h = plugin.getConfig().getDouble("aura.altura", 2.0);
        auraAngle += 0.35;

        Location base = p.getLocation();
        World w = base.getWorld();

        // Helice doble que sube y baja
        double y = (Math.sin(auraAngle * 0.5) + 1) / 2 * h;
        for (int i = 0; i < 2; i++) {
            double a = auraAngle + i * Math.PI;
            w.spawnParticle(particle, base.getX() + Math.cos(a) * r, base.getY() + y,
                    base.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        // Anillo en los pies
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + auraAngle * 0.2;
            w.spawnParticle(particle, base.getX() + Math.cos(a) * r * 1.4, base.getY() + 0.1,
                    base.getZ() + Math.sin(a) * r * 1.4, 1, 0, 0, 0, 0);
        }
    }

    private void updateGlow(Player cur) {
        if (!plugin.getConfig().getBoolean("aura.resplandor", true)) return;
        UUID id = cur == null ? null : cur.getUniqueId();
        if (Objects.equals(id, glowing)) return;
        if (glowing != null) {
            Player old = Bukkit.getPlayer(glowing);
            if (old != null) old.setGlowing(false);
        }
        glowing = id;
        if (cur != null) cur.setGlowing(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Evita que alguien se quede brillando para siempre
        if (e.getPlayer().getUniqueId().equals(glowing)) {
            e.getPlayer().setGlowing(false);
            glowing = null;
        }
    }

    // ---------- Victoria ----------
    private void win(Player p) {
        Bukkit.broadcastMessage(msg("ganador")
                .replace("%player%", p.getName()).replace("%koth%", active.name));

        rewards.give(p);

        double money = plugin.getConfig().getDouble("recompensas.dinero", 0);
        if (money > 0 && vault != null) {
            vault.give(p, money);
            p.sendMessage(color("&aRecibiste &e$" + money));
        }
        for (String cmd : plugin.getConfig().getStringList("recompensas.comandos")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
        }
        stop(false);
    }
}
