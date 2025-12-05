package com.example.bedrockac.storage;

import com.example.bedrockac.BedrockAC;
import com.example.bedrockac.player.PlayerData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final BedrockAC plugin;
    private final HikariDataSource dataSource;

    public DatabaseManager(BedrockAC plugin) {
        this.plugin = plugin;
        this.dataSource = setupDataSource();
        createTables();
    }

    private HikariDataSource setupDataSource() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "data.db");
            if (!dbFile.exists()) {
                if (!dbFile.getParentFile().exists()) {
                    dbFile.getParentFile().mkdirs();
                }
                dbFile.createNewFile();
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setPoolName("BedrockAC-SQLite");
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(10);
            config.setConnectionTestQuery("SELECT 1");

            return new HikariDataSource(config);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create database file!");
            throw new RuntimeException(e);
        }
    }

    private void createTables() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS bedrockac_logs (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "uuid VARCHAR(36) NOT NULL," +
                             "check_name VARCHAR(32) NOT NULL," +
                             "violations INTEGER NOT NULL," +
                             "timestamp BIGINT NOT NULL" +
                             ");"
             )) {
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create database tables!");
            e.printStackTrace();
        }
    }

    public void logViolation(PlayerData data, String checkName) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO bedrockac_logs (uuid, check_name, violations, timestamp) VALUES (?, ?, ?, ?);"
                 )) {
                statement.setString(1, data.getUuid().toString());
                statement.setString(2, checkName);
                statement.setInt(3, data.getViolations(checkName));
                statement.setLong(4, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Integer> getViolations(UUID uuid, String checkName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT SUM(violations) FROM bedrockac_logs WHERE uuid = ? AND check_name = ?;"
                 )) {
                statement.setString(1, uuid.toString());
                statement.setString(2, checkName);
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        });
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
