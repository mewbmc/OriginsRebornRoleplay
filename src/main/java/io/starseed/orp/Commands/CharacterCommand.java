package io.starseed.orp.Commands;

import io.starseed.orp.OriginsRebornRoleplay;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CharacterCommand implements CommandExecutor {
    private final OriginsRebornRoleplay plugin;

    public CharacterCommand(OriginsRebornRoleplay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Show current player's character
            String selectedCharacter = plugin.getSelectedCharacter(player.getUniqueId());
            if (selectedCharacter != null) {
                sender.sendMessage("Your current character is: " + selectedCharacter);
            } else {
                sender.sendMessage("You haven't selected a character yet.");
            }
        } else if (args.length == 1) {
            // Select character for the current player
            plugin.selectCharacter(player, args[0]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("view")) {
            // View another player's character
            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer != null) {
                String selectedCharacter = plugin.getSelectedCharacter(targetPlayer.getUniqueId());
                if (selectedCharacter != null) {
                    sender.sendMessage(targetPlayer.getName() + "'s current character is: " + selectedCharacter);
                } else {
                    sender.sendMessage(targetPlayer.getName() + " hasn't selected a character yet.");
                }
            } else {
                sender.sendMessage("Player not found: " + args[1]);
            }
        } else {
            sender.sendMessage("Usage: /character [character name] or /character view [player name]");
        }

        return true;
    }
}
