// src/main/java/io/starseed/orp/Utils/Character.java
package io.starseed.orp.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Character {
    private int id;
    private String name;
    private Map<String, String> shortFields;
    private Map<String, String> longFields;
    private boolean dirty;

    public Character(int id, String name) {
        this.id = id;
        this.name = name;
        this.shortFields = new HashMap<>();
        this.longFields = new HashMap<>();
        this.dirty = false;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CompletableFuture<Void> setName(String name, Connection connection) {
        // Update the character name
        this.name = name;
        this.dirty = true;
        return save(connection);
    }

    public void setShortField(String fieldName, String value) {
        shortFields.put(fieldName, value);
        this.dirty = true;
    }

    public String getShortField(String fieldName) {
        return shortFields.get(fieldName);
    }

    public Map<String, String> getShortFields() {
        return shortFields;
    }

    public void setLongField(String fieldName, String value) {
        longFields.put(fieldName, value);
        this.dirty = true;
    }

    public String getLongField(String fieldName) {
        return longFields.get(fieldName);
    }

    public Map<String, String> getLongFields() {
        return longFields;
    }

    public void setField(String fieldName, String value) {
        if (shortFields.containsKey(fieldName)) {
            setShortField(fieldName, value);
        } else {
            setLongField(fieldName, value);
        }
    }

    public Map<String, String> getFields() {
        Map<String, String> allFields = new HashMap<>(shortFields);
        allFields.putAll(longFields);
        return allFields;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public CompletableFuture<Void> save(Connection connection) {
        return CompletableFuture.runAsync(() -> {
            String updateCharacterSQL = "UPDATE characters SET character_name = ? WHERE id = ?";
            String updateFieldSQL = "INSERT INTO character_fields (character_id, field_name, field_value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE field_value = VALUES(field_value)";

            try (PreparedStatement characterStmt = connection.prepareStatement(updateCharacterSQL, PreparedStatement.RETURN_GENERATED_KEYS);
                 PreparedStatement fieldStmt = connection.prepareStatement(updateFieldSQL)) {
                // Update the character name
                characterStmt.setString(1, name);
                characterStmt.setInt(2, id);
                characterStmt.executeUpdate();

                try (ResultSet generatedKeys = characterStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        this.id = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Failed to get generated key for character.");
                    }
                }

                // Update character fields
                for (Map.Entry<String, String> entry : getFields().entrySet()) {
                    fieldStmt.setInt(1, id);
                    fieldStmt.setString(2, entry.getKey());
                    fieldStmt.setString(3, entry.getValue());
                    fieldStmt.addBatch();
                }
                fieldStmt.executeBatch();

                dirty = false;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
