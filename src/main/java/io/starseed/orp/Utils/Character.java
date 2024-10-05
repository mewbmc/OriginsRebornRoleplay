package io.starseed.orp.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Character {
    private int id;
    private String name;
    private Map<String, String> shortFields;
    private Map<String, String> longFields;
    private boolean dirty;
    
    private static final int SHORT_FIELD_MAX_LENGTH = 255;

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
        if (value.length() > SHORT_FIELD_MAX_LENGTH) {
            throw new IllegalArgumentException("Short field value exceeds maximum length of " + SHORT_FIELD_MAX_LENGTH);
        }
        shortFields.put(fieldName, value);
        this.dirty = true;
    }

    public String getShortField(String fieldName) {
        return shortFields.get(fieldName);
    }

    public Map<String, String> getShortFields() {
        return Collections.unmodifiableMap(shortFields);
    }

    public void setLongField(String fieldName, String value) {
        longFields.put(fieldName, value);
        this.dirty = true;
    }

    public String getLongField(String fieldName) {
        return longFields.get(fieldName);
    }

    public Map<String, String> getLongFields() {
        return Collections.unmodifiableMap(longFields);
    }

    public void setField(String fieldName, String value) {
        if (value.length() <= SHORT_FIELD_MAX_LENGTH) {
            setShortField(fieldName, value);
        } else {
            setLongField(fieldName, value);
        }
    }

    public Map<String, String> getAllFields() {
        Map<String, String> allFields = new HashMap<>(shortFields);
        allFields.putAll(longFields);
        return Collections.unmodifiableMap(allFields);
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
            String updateShortFieldSQL = "INSERT INTO character_short_fields (character_id, field_name, field_value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE field_value = VALUES(field_value)";
            String updateLongFieldSQL = "INSERT INTO character_long_fields (character_id, field_name, field_value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE field_value = VALUES(field_value)";

            try (PreparedStatement characterStmt = connection.prepareStatement(updateCharacterSQL);
                 PreparedStatement shortFieldStmt = connection.prepareStatement(updateShortFieldSQL);
                 PreparedStatement longFieldStmt = connection.prepareStatement(updateLongFieldSQL)) {
                // Update the character name
                characterStmt.setString(1, name);
                characterStmt.setInt(2, id);
                characterStmt.executeUpdate();

                // Update character fields
                for (Map.Entry<String, String> entry : shortFields.entrySet()) {
                    shortFieldStmt.setInt(1, id);
                    shortFieldStmt.setString(2, entry.getKey());
                    shortFieldStmt.setString(3, entry.getValue());
                    shortFieldStmt.addBatch();
                }
                shortFieldStmt.executeBatch();

                for (Map.Entry<String, String> entry : longFields.entrySet()) {
                    longFieldStmt.setInt(1, id);
                    longFieldStmt.setString(2, entry.getKey());
                    longFieldStmt.setString(3, entry.getValue());
                    longFieldStmt.addBatch();
                }
                longFieldStmt.executeBatch();

                dirty = false;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
