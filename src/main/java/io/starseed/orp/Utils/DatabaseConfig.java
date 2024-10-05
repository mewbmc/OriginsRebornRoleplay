package io.starseed.orp.Utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;

public class DatabaseConfig {
private static HikariDataSource dataSource;

// is there a better way to do this? I'm sure. can I be asked? no not at all.
public static void initialize(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        HikariConfig hikariConfig = new HikariConfig();

        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
                config.getString("database.host"),
                config.getInt("database.port"),
                config.getString("database.name"));

        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getString("database.username"));
        hikariConfig.setPassword(config.getString("database.password"));
        hikariConfig.setMaximumPoolSize(10);

        try {
            dataSource = new HikariDataSource(hikariConfig);
            plugin.getLogger().info("Successfully connected to the database.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database connection: " + e.getMessage());
            e.printStackTrace();
        }
}
public static DataSource getDataSource() {
    return dataSource;
}
}