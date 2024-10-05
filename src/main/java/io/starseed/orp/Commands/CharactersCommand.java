package io.starseed.orp.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import io.starseed.orp.OriginsRebornRoleplay;
import io.starseed.orp.Utils.Character;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CharactersCommand implements CommandExecutor {
    private final OriginsRebornRoleplay plugin;

    public CharactersCommand(OriginsRebornRoleplay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("originsreborn.character.list")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to view characters!");
            return true;
        }

        // Create inventory GUI
        Inventory gui = plugin.getServer().createInventory(null, 54, ChatColor.DARK_PURPLE + "Your Characters");

        Map<String, Character> characters = plugin.getPlayerCharacters(player.getUniqueId());
        int slot = 0;

        for (Character character : characters.values()) {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + character.getName());
                List<String> lore = new ArrayList<>();

                // Add character fields to lore
                for (Map.Entry<String, String> field : character.getFields().entrySet()) {
                    lore.add(ChatColor.GRAY + field.getKey() + ": " + ChatColor.WHITE + field.getValue());
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            gui.setItem(slot++, item);
        }

        // Add create new character item if player hasn't reached limit
        int maxCharacters = plugin.getConfig().getInt("limits.max-characters", 3);
        if (characters.size() < maxCharacters) {
            ItemStack newCharItem = new ItemStack(Material.EMERALD);
            ItemMeta meta = newCharItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + "Create New Character");
                meta.setLore(List.of(ChatColor.GRAY + "Click to create a new character"));
                newCharItem.setItemMeta(meta);
            }
            gui.setItem(53, newCharItem);
        }

        player.openInventory(gui);
        return true;
    }
}

