package io.starseed.orp.Utils;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.json.simple.JSONObject;

public class DiscordWebhook {
    private final String url;
    private static final String USER_AGENT = "OriginsRebornRoleplay/1.0";

    public DiscordWebhook(String url) {
        this.url = url;
    }

    public void send(String message) throws Exception {
        URL url = new URL(this.url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setDoOutput(true);

        // Create JSON payload
        JSONObject json = new JSONObject();
        json.put("content", message);

        // Convert to bytes for sending
        byte[] input = json.toJSONString().getBytes(StandardCharsets.UTF_8);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(input, 0, input.length);
        }

        // Check response
        int responseCode = conn.getResponseCode();
        if (responseCode != 204) { // Discord returns 204 No Content on success
            throw new Exception("Failed to send Discord webhook: HTTP " + responseCode);
        }
    }

    public void sendEmbed(String title, String description, String color) throws Exception {
        URL url = new URL(this.url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setDoOutput(true);

        // Create embed JSON structure
        JSONObject embed = new JSONObject();
        embed.put("title", title);
        embed.put("description", description);
        if (color != null) {
            embed.put("color", Integer.parseInt(color.replace("#", ""), 16));
        }

        JSONObject json = new JSONObject();
        json.put("embeds", List.of(embed));

        byte[] input = json.toJSONString().getBytes(StandardCharsets.UTF_8);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 204) {
            throw new Exception("Failed to send Discord webhook: HTTP " + responseCode);
        }
    }
}