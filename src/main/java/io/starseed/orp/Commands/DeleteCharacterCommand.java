package io.starseed.orp.Commands;

import io.starseed.orp.Utils.Character;
import io.starseed.orp.OriginsRebornRoleplay;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DeleteCharacterCommand implements CommandExecutor {
    private final OriginsRebornRoleplay plugin;

    public DeleteCharacterCommand(OriginsRebornRoleplay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("originsreborn.character.delete")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to delete characters!");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /delcharacter <name>");
            return true;
        }

        String characterName = args[0];
        Map<String, Character> characters = plugin.getPlayerCharacters(player.getUniqueId());

        if (!characters.containsKey(characterName)) {
            player.sendMessage(ChatColor.RED + "Character not found!");
            return true;
        }

        // Delete character asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                deleteCharacter(player, characterName);
                player.sendMessage(ChatColor.GREEN + "Character " + characterName + " deleted successfully!");
            } catch (SQLException e) {
                player.sendMessage(ChatColor.RED + "Failed to delete character. Please try again later.");
                plugin.getLogger().severe("Error deleting character: " + e.getMessage());
            }
        });

        return true;
    }

    private void deleteCharacter(Player player, String characterName) throws SQLException {
        Character character = plugin.getPlayerCharacters(player.getUniqueId()).get(characterName);
        if (character == null) return;

        String sql = "DELETE FROM characters WHERE id = ? AND player_uuid = ?";
        try (PreparedStatement stmt = plugin.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, character.getId());
            stmt.setString(2, player.getUniqueId().toString());
            stmt.executeUpdate();
            plugin.getPlayerCharacters(player.getUniqueId()).remove(characterName);
        }
    }
}
