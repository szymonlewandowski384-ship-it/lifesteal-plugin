package com.lifesteal.plugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.block.Action;

public class HeartListener implements Listener {
    private final LifestealPlugin plugin;
    private final NamespacedKey keyDropped;
    private final NamespacedKey keyCreated;

    public HeartListener(LifestealPlugin plugin) {
        this.plugin = plugin;
        keyDropped = new NamespacedKey(plugin, "lifesteal_dropped");
        keyCreated = new NamespacedKey(plugin, "lifesteal_created");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        int current = plugin.getHearts(victim.getUniqueId());
        // If player has 1 heart or less -> set to spectator and stop processing
        if (current <= plugin.getConfig().getInt("min-hearts", 1)) {
            Bukkit.getScheduler().runTask(plugin, () -> victim.setGameMode(GameMode.SPECTATOR));
            return;
        }

        // decrease hearts by 1 and drop item
        plugin.removeHearts(victim.getUniqueId(), 1);

        ItemStack heart = createHeartItem(false);
        victim.getWorld().dropItemNaturally(victim.getLocation(), heart);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null) return;
        ItemStack item = event.getItem();
        if (item.getType() != Material.valueOf(plugin.getConfig().getString("heart-item.material", "RED_DYE"))) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        boolean isLifesteal = pdc.has(keyDropped, PersistentDataType.BYTE) || pdc.has(keyCreated, PersistentDataType.BYTE);
        if (!isLifesteal) return;

        Player player = event.getPlayer();

        boolean created = pdc.has(keyCreated, PersistentDataType.BYTE);

        int playerHearts = plugin.getHearts(player.getUniqueId());

        // If created heart - can only deposit if player has < 10 hearts
        if (created && playerHearts >= 10) {
            player.sendMessage("§cTo stworzone serce mozna wplacic tylko gdy masz mniej niz 10 serc.");
            return;
        }

        // deposit: increase player's hearts by 1 (2 HP)
        plugin.addHearts(player.getUniqueId(), 1);
        player.sendMessage("§aWplaciles serce! Twoje maks serc: " + plugin.getHearts(player.getUniqueId()));

        // remove one item from hand
        int amount = item.getAmount();
        if (amount > 1) item.setAmount(amount - 1);
        else player.getInventory().removeItem(item);
        event.setCancelled(true);
    }

    // Utility to create a heart item. 'created' flag marks crafted hearts.
    public ItemStack createHeartItem(boolean created) {
        ItemStack it = new ItemStack(Material.valueOf(plugin.getConfig().getString("heart-item.material", "RED_DYE")), 1);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(plugin.getConfig().getString("heart-item.display-name", "&cSerce"));
        meta.setLore(plugin.getConfig().getStringList("heart-item.lore"));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (created) pdc.set(keyCreated, PersistentDataType.BYTE, (byte)1);
        else pdc.set(keyDropped, PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(meta);
        return it;
    }
}
