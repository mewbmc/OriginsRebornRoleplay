// src/main/java/io/starseed/orp/Utils/PAPIIntegration.java
package io.starseed.orp.Utils;

import io.starseed.orp.OriginsRebornRoleplay;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PAPIIntegration extends PlaceholderExpansion {
    private final OriginsRebornRoleplay plugin;

    public PAPIIntegration(OriginsRebornRoleplay plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "orp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "YourName";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        Map<String, Character> characters = plugin.getPlayerCharacters(player.getUniqueId());
        if (characters == null || characters.isEmpty()) {
            return "No character";
        }

        Character character = characters.values().iterator().next();

        // %orp_character_name%
        if (identifier.equals("character_name")) {
            return character.getName();
        }

        // %orp_character_short_<field>%
        if (identifier.startsWith("character_short_")) {
            String fieldName = identifier.substring("character_short_".length());
            return character.getShortField(fieldName);
        }

        // %orp_character_long_<field>%
        if (identifier.startsWith("character_long_")) {
            String fieldName = identifier.substring("character_long_".length());
            return character.getLongField(fieldName);
        }

        return null;
    }
}