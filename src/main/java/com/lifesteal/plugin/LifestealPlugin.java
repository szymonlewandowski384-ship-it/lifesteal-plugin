package com.lifesteal.plugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.Recipe;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LifestealPlugin extends JavaPlugin {
    private File storageFile;
    private FileConfiguration storage;

    // map UUID -> hearts count (1 heart = 2 HP)
    private Map<UUID, Integer> hearts = new HashMap<>();

    private static LifestealPlugin instance;

    public static LifestealPlugin getInstance(){ return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // storage
        storageFile = new File(getDataFolder(), getConfig().getString("storage-file", "players.yml"));
        if (!storageFile.exists()) {
            storageFile.getParentFile().mkdirs();
            try {
                storageFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create storage file: " + e.getMessage());
            }
        }
        storage = YamlConfiguration.loadConfiguration(storageFile);
        loadHearts();

        // register listeners and commands
        getServer().getPluginManager().registerEvents(new HeartListener(this), this);
        Commands commands = new Commands(this);
        if (getCommand("withdrawheart") != null) getCommand("withdrawheart").setExecutor(commands);
        if (getCommand("createheart") != null) getCommand("createheart").setExecutor(commands);
        if (getCommand("revive") != null) getCommand("revive").setExecutor(commands);

        // register crafting recipe
        registerCraftingRecipe();

        // ensure online players have correct max health shortly after enable
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Map.Entry<UUID, Integer> e : hearts.entrySet()) {
                Player p = Bukkit.getPlayer(e.getKey());
                if (p != null) p.setMaxHealth(e.getValue() * 2);
            }
        }, 20L);

        getLogger().info("Lifesteal enabled");
    }

    @Override
    public void onDisable() {
        saveHearts();
        getLogger().info("Lifesteal disabled");
    }

    public int getHearts(UUID uuid) {
        return hearts.getOrDefault(uuid, getConfig().getInt("default-hearts", 10)); // default
    }

    public void setHearts(UUID uuid, int count) {
        int min = getConfig().getInt("min-hearts", 1);
        int max = getConfig().getInt("max-hearts", 20);
        if (count < min) count = min;
        if (count > max) count = max;
        hearts.put(uuid, count);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            p.setMaxHealth(count * 2);
            if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        }
    }

    public void addHearts(UUID uuid, int add) {
        int max = getConfig().getInt("max-hearts", 20);
        int newValue = getHearts(uuid) + add;
        if (newValue > max) newValue = max;
        setHearts(uuid, newValue);
    }

    public void removeHearts(UUID uuid, int rem) {
        setHearts(uuid, getHearts(uuid) - rem);
    }

    private void loadHearts() {
        if (storage.contains("players")) {
            for (String key : storage.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID u = UUID.fromString(key);
                    int v = storage.getInt("players." + key);
                    hearts.put(u, v);
                } catch (Exception e) {
                    getLogger().warning("Invalid UUID in storage: " + key);
                }
            }
        }
    }

    private void saveHearts() {
        storage.set("players", null);
        for (Map.Entry<UUID, Integer> e : hearts.entrySet()) {
            storage.set("players." + e.getKey().toString(), e.getValue());
        }
        try {
            storage.save(storageFile);
        } catch (IOException ex) {
            getLogger().severe("Could not save hearts storage: " + ex.getMessage());
        }
    }


private ItemStack createCraftedHeartItem() {
    ItemStack it = new ItemStack(Material.valueOf(getConfig().getString("created-heart-item.material", "RED_DYE")), 1);
    ItemMeta meta = it.getItemMeta();
    meta.setDisplayName(getConfig().getString("created-heart-item.display-name", "&aStworzone Serce"));
    meta.setLore(getConfig().getStringList("created-heart-item.lore"));
    // mark as created
    NamespacedKey key = new NamespacedKey(this, "lifesteal_created");
    meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte)1);
    it.setItemMeta(meta);
    return it;
}

private void registerCraftingRecipe() {
    try {
        ItemStack result = createCraftedHeartItem();
        NamespacedKey key = new NamespacedKey(this, "craft_created_heart");
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        // R G R
        // G D G
        // R G R
        recipe.shape("RGR","GDG","RGR");
        recipe.setIngredient('R', Material.valueOf(getConfig().getString("heart-item.material","RED_DYE")));
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('D', Material.DIAMOND);
        // register
        if (getServer().getRecipe(key) == null) getServer().addRecipe(recipe);
    } catch (Exception e) {
        getLogger().warning("Could not register crafted heart recipe: " + e.getMessage());
    }
}

}
