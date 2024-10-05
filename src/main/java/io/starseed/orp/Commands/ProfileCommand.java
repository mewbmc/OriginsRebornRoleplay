package io.starseed.orp.Commands;

import io.starseed.orp.OriginsRebornRoleplay;
import io.starseed.orp.Utils.Character;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class ProfileCommand implements CommandExecutor {
    private final OriginsRebornRoleplay plugin;

    public ProfileCommand(OriginsRebornRoleplay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();
        Map<String, Character> characters = plugin.getPlayerCharacters(playerUUID);

        if (characters.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You have no characters.");
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "Your Characters:");
        for (Character character : characters.values()) {
            player.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + character.getName());
            for (Map.Entry<String, String> field : character.getFields().entrySet()) {
                player.sendMessage(ChatColor.YELLOW + field.getKey() + ": " + ChatColor.WHITE + field.getValue());
            }
        }

        return true;
    }
}