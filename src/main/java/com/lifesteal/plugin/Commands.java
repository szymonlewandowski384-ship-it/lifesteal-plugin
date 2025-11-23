package com.lifesteal.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Commands implements CommandExecutor {
    private final LifestealPlugin plugin;

    public Commands(LifestealPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("withdrawheart")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Tylko gracz moze uzyc tej komendy.");
                return true;
            }
            Player p = (Player) sender;
            int hearts = plugin.getHearts(p.getUniqueId());
            if (hearts <= 10) {
                p.sendMessage(ChatColor.RED + "Mozesz wyplacic serce tylko gdy masz wiecej niz 10 serc.");
                return true;
            }
            // remove 1 heart and give item
            plugin.removeHearts(p.getUniqueId(), 1);
            ItemStack heart = new HeartListener(plugin).createHeartItem(false);
            p.getInventory().addItem(heart);
            p.sendMessage(ChatColor.GREEN + "Wyplaciles jedno serce. Obecne serca: " + plugin.getHearts(p.getUniqueId()));
            return true;
        }

        if (cmd.equals("createheart")) {
            if (!sender.hasPermission("lifesteal.create")) {
                sender.sendMessage(ChatColor.RED + "Brak permisji.");
                return true;
            }
            int amount = 1;
            if (args.length >= 1) {
                try { amount = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
            }
            if (sender instanceof Player) {
                Player p = (Player) sender;
                ItemStack created = new HeartListener(plugin).createHeartItem(true);
                created.setAmount(amount);
                p.getInventory().addItem(created);
                p.sendMessage(ChatColor.GREEN + "Stworzono " + amount + " serc.");
            } else {
                sender.sendMessage("Created " + amount + " hearts (console)");
            }
            return true;
        }

        if (cmd.equals("revive")) {
            if (!sender.hasPermission("lifesteal.revive")) {
                sender.sendMessage(ChatColor.RED + "Nie masz uprawnien.");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.YELLOW + "Użycie: /revive <gracz>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Gracz offline.");
                return true;
            }
            target.setGameMode(GameMode.SURVIVAL);
            plugin.setHearts(target.getUniqueId(), plugin.getConfig().getInt("default-hearts", 10));
            target.setHealth(target.getMaxHealth());
            sender.sendMessage(ChatColor.GREEN + "Gracz zostal wskrzeszony!");
            target.sendMessage(ChatColor.GREEN + "Zostales wskrzeszony!");
            return true;
        }

        return false;
    }
}
