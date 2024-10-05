package io.starseed.orp.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import io.starseed.orp.OriginsRebornRoleplay;
import io.starseed.orp.Utils.Character;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class NewCharacterCommand implements CommandExecutor {
    private final OriginsRebornRoleplay plugin;

    public NewCharacterCommand(OriginsRebornRoleplay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("originsreborn.character.create")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to create characters!");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /newcharacter <name>");
            return true;
        }

        String characterName = args[0];
        if (characterName.length() > 32) {
            player.sendMessage(ChatColor.RED + "Character name is too long! Maximum length is 32 characters.");
            return true;
        }

        // Check if player has reached character limit
        int maxCharacters = plugin.getConfig().getInt("limits.max-characters", 3);
        if (plugin.getPlayerCharacters(player.getUniqueId()).size() >= maxCharacters) {
            player.sendMessage(ChatColor.RED + "You have reached the maximum number of characters!");
            return true;
        }

        // Create character async
        CompletableFuture.runAsync(() -> {
            try {
                createCharacter(player, characterName);
                player.sendMessage(ChatColor.GREEN + "Character " + characterName + " created successfully!");
            } catch (SQLException e) {
                player.sendMessage(ChatColor.RED + "Failed to create character. Please try again later.");
                plugin.getLogger().severe("Error creating character: " + e.getMessage());
            }
        });

        return true;
    }

    private void createCharacter(Player player, String characterName) throws SQLException {
        String sql = "INSERT INTO characters (player_uuid, character_name) VALUES (?, ?)";
        try (PreparedStatement stmt = plugin.getConnection().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, characterName);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int characterId = rs.getInt(1);
                Character character = new Character(characterId, characterName);
                plugin.getPlayerCharacters(player.getUniqueId()).put(characterName, character);
            }
        }
    }
}