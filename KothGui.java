package com.ejemplo.koth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class KothGui implements Listener {
    private static final int SLOT_REWARDS = 28, SLOT_STOP = 31, SLOT_NEXT = 34;

    private final KothManager manager;
    private final RewardManager rewards;
    private final KothScheduler scheduler;

    public KothGui(KothManager manager, RewardManager rewards, KothScheduler scheduler) {
        this.manager = manager;
        this.rewards = rewards;
        this.scheduler = scheduler;
    }

    // ---------- Helpers ----------
    private static Component comp(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s)
                .decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(comp(name));
        if (lore.length > 0) {
            meta.lore(Arrays.stream(lore).map(KothGui::comp).collect(Collectors.toList()));
        }
        it.setItemMeta(meta);
        return it;
    }

    private List<Koth> sorted() {
        return manager.all().stream()
                .sorted(Comparator.comparing(k -> k.name.toLowerCase()))
                .collect(Collectors.toList());
    }

    // ---------- Menu principal (admin) ----------
    public void openMain(Player p) {
        KothMenu holder = new KothMenu(KothMenu.Type.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 36, comp("&6&lKOTH &8- Panel"));
        holder.setInventory(inv);

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 27; i < 36; i++) inv.setItem(i, filler);

        Koth act = manager.getActive();
        List<Koth> list = sorted();
        for (int i = 0; i < list.size() && i < 27; i++) {
            Koth k = list.get(i);
            boolean on = act != null && act.name.equalsIgnoreCase(k.name);
            inv.setItem(i, item(Material.BEACON, "&e" + k.name,
                    "&7Mundo: &f" + k.world.getName(),
                    "&7Tamano: &f" + (k.maxX - k.minX + 1) + "x" + (k.maxY - k.minY + 1) + "x" + (k.maxZ - k.minZ + 1),
                    "&7Captura: &f" + k.captureSeconds + "s",
                    on ? "&a* ACTIVO" : "&8* Inactivo",
                    " ",
                    "&eClick izq: &7iniciar",
                    "&cShift + der: &7eliminar"));
        }

        inv.setItem(SLOT_REWARDS, item(Material.CHEST, "&6Recompensas",
                "&eClick izq: &7ver", "&eClick der: &7editar"));
        inv.setItem(SLOT_STOP, manager.isRunning()
                ? item(Material.REDSTONE_BLOCK, "&cDetener KOTH", "&7Click para detener el evento")
                : item(Material.GRAY_DYE, "&7Sin KOTH activo"));
        inv.setItem(SLOT_NEXT, item(Material.CLOCK, "&bProximo evento", "&f" + scheduler.next()));

        p.openInventory(inv);
    }

    // ---------- Menu de recompensas ----------
    public void openRewards(Player p, boolean edit) {
        KothMenu holder = new KothMenu(edit ? KothMenu.Type.REWARDS_EDIT : KothMenu.Type.REWARDS_VIEW);
        Inventory inv = Bukkit.createInventory(holder, 54,
                comp(edit ? "&cEditar recompensas" : "&6Recompensas KOTH"));
        holder.setInventory(inv);

        List<ItemStack> items = rewards.getItems();
        for (int i = 0; i < items.size() && i < 54; i++) inv.setItem(i, items.get(i));

        p.openInventory(inv);
        if (edit) p.sendMessage(KothManager.color("&eColoca o quita items. Se guardan al cerrar el menu."));
    }

    public void closeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof KothMenu) p.closeInventory();
        }
    }

    // ---------- Eventos ----------
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof KothMenu menu)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (menu.type() == KothMenu.Type.REWARDS_EDIT) {
            if (!p.hasPermission("koth.admin")) e.setCancelled(true);
            return; // admin edita libremente
        }

        e.setCancelled(true); // MAIN y VIEW son solo lectura
        if (menu.type() != KothMenu.Type.MAIN) return;
        if (!p.hasPermission("koth.admin")) return;
        if (e.getRawSlot() < 0 || e.getRawSlot() >= 36) return;

        int slot = e.getRawSlot();
        List<Koth> list = sorted();

        if (slot < 27 && slot < list.size()) {
            Koth k = list.get(slot);
            if (e.getClick() == ClickType.SHIFT_RIGHT) {
                Koth act = manager.getActive();
                if (act != null && act.name.equalsIgnoreCase(k.name)) manager.stop(true);
                manager.delete(k.name);
                openMain(p);
            } else if (e.getClick().isLeftClick()) {
                if (manager.start(k)) p.closeInventory();
                else p.sendMessage(KothManager.color("&cYa hay un KOTH activo."));
            }
        } else if (slot == SLOT_REWARDS) {
            openRewards(p, e.getClick().isRightClick());
        } else if (slot == SLOT_STOP && manager.isRunning()) {
            manager.stop(true);
            openMain(p);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getInventory().getHolder() instanceof KothMenu menu)) return;
        if (menu.type() != KothMenu.Type.REWARDS_EDIT || !e.getWhoClicked().hasPermission("koth.admin")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof KothMenu menu)) return;
        if (menu.type() != KothMenu.Type.REWARDS_EDIT) return;
        if (!e.getPlayer().hasPermission("koth.admin")) return;
        rewards.save(Arrays.asList(e.getInventory().getContents()));
        e.getPlayer().sendMessage(KothManager.color("&aRecompensas guardadas."));
    }
}
