package com.ejemplo.koth;

import org.bukkit.Bukkit;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Lee la lista "horarios" del config y arranca KOTHs automaticamente. */
public class KothScheduler {
    private final KothPlugin plugin;
    private final KothManager manager;
    private String lastFired = "";

    public KothScheduler(KothPlugin plugin, KothManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        // Revisa cada 10 segundos
        Bukkit.getScheduler().runTaskTimer(plugin, this::check, 200L, 200L);
    }

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private void check() {
        LocalDateTime now = LocalDateTime.now();
        String hm = now.format(HM);
        String today = now.getDayOfWeek().name().substring(0, 3); // MON, TUE...

        for (String entry : plugin.getConfig().getStringList("horarios")) {
            String[] t = entry.trim().split("\\s+");
            String day = null, time, name;
            if (t.length == 2) { time = t[0]; name = t[1]; }
            else if (t.length == 3) { day = t[0].toUpperCase(); time = t[1]; name = t[2]; }
            else continue;

            if (day != null && !day.equals(today)) continue;
            if (!time.equals(hm)) continue;

            String key = now.toLocalDate() + " " + entry;
            if (key.equals(lastFired)) continue;

            Koth k = manager.get(name);
            if (k != null && !manager.isRunning()) {
                manager.start(k);
                lastFired = key;
            }
        }
    }

    // ---------- Lista de horarios (para /koth next y placeholders) ----------
    private record Entry(LocalDateTime when, String day, String time, String name) {}

    /** Entradas validas ordenadas por la fecha de su proxima ejecucion. */
    private List<Entry> entries() {
        LocalDateTime now = LocalDateTime.now();
        List<Entry> out = new ArrayList<>();
        for (String entry : plugin.getConfig().getStringList("horarios")) {
            String[] t = entry.trim().split("\\s+");
            String day = null, time, name;
            if (t.length == 2) { time = t[0]; name = t[1]; }
            else if (t.length == 3) { day = t[0].toUpperCase(); time = t[1]; name = t[2]; }
            else continue;
            try {
                String[] hm = time.split(":");
                for (int d = 0; d <= 7; d++) {
                    LocalDateTime c = now.plusDays(d)
                            .withHour(Integer.parseInt(hm[0])).withMinute(Integer.parseInt(hm[1]))
                            .withSecond(0).withNano(0);
                    if (c.isBefore(now)) continue;
                    if (day != null && !c.getDayOfWeek().name().substring(0, 3).equals(day)) continue;
                    out.add(new Entry(c, day, time, name));
                    break;
                }
            } catch (Exception ignored) {}
        }
        out.sort(Comparator.comparing(Entry::when));
        return out;
    }

    private String in(Entry e) {
        long mins = java.time.Duration.between(LocalDateTime.now(), e.when()).toMinutes();
        long d = mins / 1440, h = (mins % 1440) / 60, m = mins % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (d > 0 || h > 0) sb.append(h).append("h ");
        sb.append(m).append("m");
        return sb.toString();
    }

    private String label(Entry e) {
        return (e.day() == null ? "" : e.day() + " ") + e.time() + " - " + e.name();
    }

    private String none() {
        return plugin.getConfig().getString("placeholders.sin-horarios", "Sin horarios");
    }

    /** Texto con el proximo horario programado. */
    public String next() {
        List<Entry> l = entries();
        return l.isEmpty() ? none() : l.get(0).name() + " en " + in(l.get(0));
    }

    public String nextName() { List<Entry> l = entries(); return l.isEmpty() ? none() : l.get(0).name(); }
    public String nextTime() { List<Entry> l = entries(); return l.isEmpty() ? none() : (l.get(0).day() == null ? "" : l.get(0).day() + " ") + l.get(0).time(); }
    public String nextIn()   { List<Entry> l = entries(); return l.isEmpty() ? none() : in(l.get(0)); }

    public int count() { return entries().size(); }

    /** Linea N (1 = el mas cercano) con formato "SAT 21:30 - spawn". */
    public String line(int n) {
        List<Entry> l = entries();
        return (n >= 1 && n <= l.size()) ? label(l.get(n - 1)) : "";
    }

    /** Igual que line() pero con el tiempo restante: "SAT 21:30 - spawn (2h 10m)". */
    public String lineIn(int n) {
        List<Entry> l = entries();
        return (n >= 1 && n <= l.size()) ? label(l.get(n - 1)) + " (" + in(l.get(n - 1)) + ")" : "";
    }
}
