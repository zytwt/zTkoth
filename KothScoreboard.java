package com.ejemplo.koth;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

public class KothScoreboard {
    private final Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
    private final Objective obj;
    private final Map<UUID, Scoreboard> previous = new HashMap<>();

    public KothScoreboard(String title) {
        obj = board.registerNewObjective("koth", Criteria.DUMMY,
                LegacyComponentSerializer.legacySection().deserialize(title));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void show(Player p) {
        if (previous.containsKey(p.getUniqueId())) return;
        previous.put(p.getUniqueId(), p.getScoreboard());
        p.setScoreboard(board);
    }

    public void update(List<String> lines) {
        for (String e : new HashSet<>(board.getEntries())) board.resetScores(e);
        Set<String> used = new HashSet<>();
        int score = lines.size();
        for (String line : lines) {
            String entry = line;
            while (!used.add(entry)) entry += "§r"; // evita entradas repetidas
            obj.getScore(entry).setScore(score--);
        }
    }

    public void hideAll() {
        for (Map.Entry<UUID, Scoreboard> e : previous.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null) p.setScoreboard(e.getValue());
        }
        previous.clear();
        obj.unregister();
    }
}
