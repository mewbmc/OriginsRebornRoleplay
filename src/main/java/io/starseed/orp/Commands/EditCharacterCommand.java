package io.starseed.orp.Commands;

import io.starseed.orp.OriginsRebornRoleplay;
import io.starseed.orp.Utils.Character;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class EditCharacterCommand implements CommandExecutor, TabCompleter {
    private final OriginsRebornRoleplay plugin;
    private final List<String> EDITABLE_FIELDS = Arrays.asList(
            "name", "age", "gender", "race", "occupation", "description", "backstory"
    );

    public EditCharacterCommand(OriginsRebornRoleplay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /editcharacter <character> <field>");
            player.sendMessage(ChatColor.YELLOW + "Available fields: " + String.join(", ", EDITABLE_FIELDS));
            return true;
        }

        String characterName = args[0];
        String fieldName = args[1].toLowerCase();

        // Validate field name
        if (!EDITABLE_FIELDS.contains(fieldName)) {
            player.sendMessage(ChatColor.RED + "Invalid field: " + fieldName);
            player.sendMessage(ChatColor.YELLOW + "Available fields: " + String.join(", ", EDITABLE_FIELDS));
            return true;
        }

        // Get character
        Map<String, Character> characters = plugin.getPlayerCharacters(player.getUniqueId());
        if (!characters.containsKey(characterName)) {
            player.sendMessage(ChatColor.RED + "Character not found: " + characterName);
            return true;
        }

        Character character = characters.get(characterName);

        // Get current value
        String currentValue = fieldName.equals("name") ?
                character.getName() :
                character.getShortField(fieldName);

        if (currentValue == null) {
            currentValue = "";
        }

        // Create and open the AnvilGUI with updated builder pattern
        new AnvilGUI.Builder()
                .onClose(stateSnapshot -> {
                    // Handle close event if needed
                })
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String text = stateSnapshot.getText();
                    String trimmedText = text.trim();

                    // Validate input
                    if (trimmedText.isEmpty()) {
                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("The " + fieldName + " cannot be empty!"));
                    }

                    if (trimmedText.length() > 100) {
                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("Maximum 100 characters!"));
                    }

                    if (fieldName.equals("name") && characters.containsKey(trimmedText)) {
                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("Name already taken!"));
                    }

                    // Save changes and close
                    return Arrays.asList(
                            AnvilGUI.ResponseAction.close(),
                            AnvilGUI.ResponseAction.run(() -> saveCharacterField(player, character, fieldName, trimmedText))
                    );
                })
                .text(currentValue)
                .title(ChatColor.DARK_PURPLE + "Edit " + fieldName)
                .plugin(plugin)
                .open(player);

        return true;
    }

    private void saveCharacterField(Player player, Character character, String fieldName, String newValue) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = plugin.getConnection()) {
                CompletableFuture<?> future;

                if (fieldName.equals("name")) {
                    // Handle name change
                    future = character.setName(newValue, connection)
                            .thenRun(() -> {
                                // Update the character map with the new name
                                Map<String, Character> characters = plugin.getPlayerCharacters(player.getUniqueId());
                                characters.remove(character.getName());
                                characters.put(newValue, character);
                            });
                } else {
                    // Handle other fields
                    character.setShortField(fieldName, newValue);
                    future = character.save(connection);
                }

                // Handle completion
                future.thenRun(() -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(ChatColor.GREEN + "Successfully updated " + fieldName + " to: " + newValue);

                        // If this was the selected character and the name changed, update the selection
                        if (fieldName.equals("name") &&
                                character.getName().equals(plugin.getSelectedCharacter(player.getUniqueId()))) {
                            plugin.selectCharacter(player, newValue);
                        }
                    });
                }).exceptionally(ex -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(ChatColor.RED + "Failed to update " + fieldName + ": " + ex.getMessage());
                    });
                    plugin.getLogger().severe("Error updating " + fieldName + " for character " +
                            character.getName() + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
            } catch (SQLException e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "Failed to update " + fieldName + ": Database error");
                });
                plugin.getLogger().severe("Database error while updating " + fieldName + " for character " +
                        character.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            // First argument - character names
            return new ArrayList<>(plugin.getPlayerCharacters(player.getUniqueId()).keySet()).stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Second argument - field names
            return EDITABLE_FIELDS.stream()
                    .filter(field -> field.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}