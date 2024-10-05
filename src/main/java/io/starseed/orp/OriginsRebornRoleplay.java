package io.starseed.orp;

import io.starseed.orp.Utils.*;
import io.starseed.orp.Commands.*;
import io.starseed.orp.Utils.Character;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.Objects;

public class OriginsRebornRoleplay extends JavaPlugin implements Listener {
    private Map<UUID, String> selectedCharacters = new HashMap<>();
    private Map<UUID, Map<String, io.starseed.orp.Utils.Character>> playerCharacters;
    private FileConfiguration config;
    private DiscordWebhook discordWebhook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        playerCharacters = new HashMap<>();
        selectedCharacters = new HashMap<>();
        DatabaseConfig.initialize(this);
        setupDatabase();
        setupDiscordWebhook();
        getServer().getPluginManager().registerEvents(this, this);
        registerCommands();


        // PlaceholderAPI integration
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIIntegration(this).register();
        }

        getLogger().info("Origins Reborn Roleplay has been enabled!");
        startPeriodicSaving();
    }

    @Override
    public void onDisable() {
        saveAllCharacterData();
        getLogger().info("Origins Reborn Roleplay has been disabled!");
    }

    private void setupDatabase() {
        try (Connection connection = DatabaseConfig.getDataSource().getConnection()) {
            createTables(connection);
        } catch (SQLException e) {
            getLogger().severe("Failed to connect to database: " + e.getMessage());
        }
    }

    private void createTables(Connection connection) throws SQLException {
        String characterTableSQL = """
                CREATE TABLE IF NOT EXISTS characters (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    player_uuid VARCHAR(36) NOT NULL,
                    character_name VARCHAR(32) NOT NULL,
                    created_at DATETIME,
                    UNIQUE (player_uuid, character_name)
                )
                """;
        String characterFieldsSQL = """
    CREATE TABLE IF NOT EXISTS character_fields (
        character_id INT,
        field_name VARCHAR(32) NOT NULL,
        field_value TEXT,
        FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE,
        PRIMARY KEY (character_id, field_name)
    )
    """;

        try (PreparedStatement stmt = connection.prepareStatement(characterTableSQL)) {
            stmt.execute();
        }

        try (PreparedStatement stmt = connection.prepareStatement(characterFieldsSQL)) {
            stmt.execute();
        }
    }

    private void setupDiscordWebhook() {
        String webhookUrl = config.getString("discord.webhook-url");
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            discordWebhook = new DiscordWebhook(webhookUrl);
        }
    }

    private void registerCommands() {
        getCommand("rpprofile").setExecutor(new ProfileCommand(this));
        getCommand("newcharacter").setExecutor(new NewCharacterCommand(this));
        getCommand("characters").setExecutor(new CharactersCommand(this));
        getCommand("delcharacter").setExecutor(new DeleteCharacterCommand(this));
        getCommand("character").setExecutor(new CharacterCommand(this));
        getCommand("editcharacter").setExecutor(new EditCharacterCommand(this));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadPlayerCharacters(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        savePlayerCharacters(event.getPlayer());
        playerCharacters.remove(event.getPlayer().getUniqueId());
    }

    public CompletableFuture<Void> loadPlayerCharacters(Player player) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = DatabaseConfig.getDataSource().getConnection()) {
                String sql = "SELECT c.*, cf.field_name, cf.field_value FROM characters c " +
                        "LEFT JOIN character_fields cf ON c.id = cf.character_id " +
                        "WHERE c.player_uuid = ?";

                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, player.getUniqueId().toString());
                    ResultSet rs = stmt.executeQuery();

                    Map<String, io.starseed.orp.Utils.Character> characters = new HashMap<>();
                    while (rs.next()) {
                        String characterName = rs.getString("character_name");
                        io.starseed.orp.Utils.Character character = characters.computeIfAbsent(characterName,
                                k -> {
                                    try {
                                        return new io.starseed.orp.Utils.Character(rs.getInt("id"), characterName);
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                });

                        String fieldName = rs.getString("field_name");
                        String fieldValue = rs.getString("field_value");
                        if (fieldName != null) {
                            character.setField(fieldName, fieldValue);
                        }
                    }

                    playerCharacters.put(player.getUniqueId(), characters);
                }
            } catch (SQLException e) {
                getLogger().severe("Error loading characters for " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> savePlayerCharacters(Player player) {
        return CompletableFuture.runAsync(() -> {
            Map<String, io.starseed.orp.Utils.Character> characters = playerCharacters.get(player.getUniqueId());
            if (characters == null) return;

            try (Connection connection = DatabaseConfig.getDataSource().getConnection()) {
                List<CompletableFuture<Void>> saveFutures = new ArrayList<>();
                for (io.starseed.orp.Utils.Character character : characters.values()) {
                    if (character.isDirty()) {
                        saveFutures.add(character.save(connection));
                    }
                }
                CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0])).join();
            } catch (SQLException e) {
                getLogger().severe("Error saving characters for " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    private void saveAllCharacterData() {
        for (UUID playerUUID : playerCharacters.keySet()) {
            try (Connection connection = DatabaseConfig.getDataSource().getConnection()) {
                for (io.starseed.orp.Utils.Character character : playerCharacters.get(playerUUID).values()) {
                    if (character.isDirty()) {
                        updateCharacter(connection, playerUUID, character);
                    }
                }
            } catch (SQLException e) {
                getLogger().severe("Error saving character data: " + e.getMessage());
            }
        }
    }
    private void updateCharacter(Connection connection, UUID playerUUID, Character character) throws SQLException {
        String updateCharacterSQL = "UPDATE characters SET last_updated = CURRENT_TIMESTAMP WHERE id = ?";
        String updateFieldSQL = "INSERT INTO character_fields (character_id, field_name, field_value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE field_value = VALUES(field_value)";

        try (PreparedStatement characterStmt = connection.prepareStatement(updateCharacterSQL);
              PreparedStatement fieldStmt = connection.prepareStatement(updateFieldSQL)) {
            // Update the character name
            characterStmt.setInt(1, character.getId());
            characterStmt.executeUpdate();

            // Update character fields
            for (Map.Entry<String, String> field : character.getFields().entrySet()) {
                fieldStmt.setInt(1, character.getId());
                fieldStmt.setString(2, field.getKey());
                fieldStmt.setString(3, field.getValue());
                fieldStmt.addBatch();
            }
            fieldStmt.executeBatch();
        } catch (SQLException e) {
            throw e;
        }

        if (discordWebhook != null) {
            updateDiscordLore(character);
        }
    }
    private void updateDiscordLore(io.starseed.orp.Utils.Character character) {
        StringBuilder message = new StringBuilder();
        message.append("**Character Update: ").append(character.getName()).append("**\n\n");

        for (Map.Entry<String, String> field : character.getFields().entrySet()) {
            message.append("**").append(field.getKey()).append("**: ")
                    .append(field.getValue()).append("\n");
        }

        CompletableFuture.runAsync(() -> {
            try {
                discordWebhook.send(message.toString());
            } catch (Exception e) {
                getLogger().warning("Failed to send character update to Discord: " + e.getMessage());
            }
        });
    }

    public Map<String, io.starseed.orp.Utils.Character> getPlayerCharacters(UUID playerUUID) {
        return playerCharacters.getOrDefault(playerUUID, new HashMap<>());
    }

    public Connection getConnection() throws SQLException {
        return Objects.requireNonNull(DatabaseConfig.getDataSource()).getConnection();
    }

    private void startPeriodicSaving() {
        long saveInterval = getConfig().getLong("save-interval", 5) * 20L * 60L; // Convert minutes to ticks
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                savePlayerCharacters(player);
            }
        }, saveInterval, saveInterval);
    }

    public String getSelectedCharacter(UUID playerUUID) {
        return selectedCharacters.get(playerUUID);
    }
    public void selectCharacter(Player player, String characterName) {
        UUID playerUUID = player.getUniqueId();
        Map<String, Character> characters = getPlayerCharacters(playerUUID);
        if (characters.containsKey(characterName)) {
            selectedCharacters.put(playerUUID, characterName);
            player.sendMessage("You have selected the character: " + characterName);
        } else {
            player.sendMessage("Character not found: " + characterName);
        }
    }
}
