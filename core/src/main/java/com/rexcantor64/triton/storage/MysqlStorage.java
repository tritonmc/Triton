package com.rexcantor64.triton.storage;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.utils.JSONUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.val;
import lombok.var;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.util.UUID;

public class MysqlStorage implements Storage {

    private HikariConfig config = new HikariConfig();
    private HikariDataSource dataSource;
    private String host;
    private int port;
    private String database;
    private String user;
    private String password;
    private String tablePrefix;

    private IpCache ipCache;

    public MysqlStorage(String host, int port, String database, String user, String password, String tablePrefix) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.tablePrefix = tablePrefix;
        config.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL" +
                "=false");
        config.setUsername(user);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.dataSource = new HikariDataSource(config);

        this.ipCache = new IpCache();
    }

    private Connection openConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    public boolean setup() {
        try (Connection connection = openConnection()) {
            Statement stmt = connection.createStatement();
            // TODO sanitize tablePrefix
            stmt.execute("CREATE TABLE IF NOT EXISTS `" + tablePrefix + "player_data` ( `key` VARCHAR(39) NOT NULL , " +
                    "`value` VARCHAR(100) NOT NULL, PRIMARY KEY (`key`) );");
            stmt.execute("CREATE TABLE IF NOT EXISTS `" + tablePrefix + "collections` ( `name` VARCHAR(100) NOT NULL " +
                    ", `servers` TEXT NOT NULL , `blacklist` BOOLEAN NOT NULL , PRIMARY KEY (`name`));");
            stmt.execute("CREATE TABLE IF NOT EXISTS `" + tablePrefix + "translations` ( `id` INT NOT NULL " +
                    "AUTO_INCREMENT , `collection` VARCHAR(100) NOT NULL , `type` ENUM('text','sign') NOT NULL " +
                    "DEFAULT 'text' , `field_key` VARCHAR(200) NOT NULL , `content` MEDIUMTEXT NOT NULL , `blacklist`" +
                    " BOOLEAN NULL DEFAULT NULL , `servers` TEXT NULL DEFAULT NULL , `locations` MEDIUMTEXT NULL " +
                    "DEFAULT NULL , `patterns` TEXT NULL DEFAULT NULL , `twin_id` VARCHAR(36) NOT NULL , `twin_data` " +
                    "TEXT NOT NULL , `archived` BOOLEAN NOT NULL , PRIMARY KEY (`id`), UNIQUE (`twin_id`) , " +
                    "CONSTRAINT `collections_translations` FOREIGN KEY (`collection`) REFERENCES " +
                    "`triton_collections`(`name`) ON DELETE RESTRICT ON UPDATE CASCADE);");
            stmt.close();
            return true;
        } catch (SQLException e) {
            Triton.get().getLogger().logError("Error connecting to database: %1", e.getMessage());
        }
        return false;
    }

    @Override
    public Language getLanguageFromIp(String ip) {
        String lang = ipCache.getFromCache(ip);
        if (lang == null) {
            lang = getValueFromStorage(ip);
            ipCache.addToCache(ip, lang == null ? "" : lang);
        }

        return Triton.get().getLanguageManager().getLanguageByName(lang, true);
    }

    @Override
    public Language getLanguage(LanguagePlayer lp) {
        String lang = getValueFromStorage(lp.getUUID().toString());
        if (!Triton.get().getConf().isBungeecord() &&
                (lang == null
                        || (Triton.get().getConf().isAlwaysCheckClientLocale())))
            lp.waitForClientLocale();
        return Triton.get().getLanguageManager().getLanguageByName(lang, true);
    }

    @Override
    public void setLanguage(UUID uuid, String ip, Language newLanguage) {
        String entity = uuid != null ? uuid.toString() : ip;
        if (uuid == null && ip == null) return;
        Triton.get().getLogger().logDebug("Saving language for %1...", entity);
        try (Connection connection = openConnection()) {
            PreparedStatement stmt = connection
                    .prepareStatement("INSERT INTO `" + tablePrefix + "player_data` (`key`, `value`) VALUES (?, ?) ON" +
                            " DUPLICATE KEY UPDATE value=VALUES(value)");
            stmt.setString(2, newLanguage.getName());
            if (uuid != null) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }
            if (ip != null) {
                stmt.setString(1, ip);
                stmt.executeUpdate();
            }
            stmt.close();
            Triton.get().getLogger().logDebug("Saved!");
        } catch (Exception e) {
            e.printStackTrace();
            Triton.get().getLogger()
                    .logError("Failed to save language for %1! Could not insert into database: %2", entity, e
                            .getMessage());
        }
    }

    private String getValueFromStorage(String key) {
        String result = null;
        try (Connection connection = openConnection()) {
            PreparedStatement stmt = connection
                    .prepareStatement("SELECT `value` FROM `" + tablePrefix + "player_data` WHERE `key`=?");
            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                result = rs.getString(1);
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            Triton.get().getLogger().logError("Failed to get value from the database: %1", e.getMessage());
        }
        return result;
    }

    @Override
    public boolean uploadToStorage(@NonNull JSONObject metadata, @NonNull JSONArray items) {
        try {
            @Cleanup val connection = openConnection();

            @Cleanup val emptyTablesStatement = connection.createStatement();
            emptyTablesStatement.execute("TRUNCATE `" + tablePrefix + "translations`");
            emptyTablesStatement.execute("DELETE FROM `" + tablePrefix + "collections`");

            @Cleanup val collectionsStatement = connection
                    .prepareStatement("INSERT INTO `" + tablePrefix + "collections` (`name`, `servers`, `blacklist`) " +
                            "VALUES (?, ?, ?);");
            for (val collectionName : metadata.keySet()) {
                val obj = metadata.optJSONObject(collectionName);
                collectionsStatement.setString(1, collectionName);

                val blacklist = obj.optBoolean("blacklist", true);
                val servers = obj.optJSONArray("servers");
                collectionsStatement.setString(2, servers.toString());
                collectionsStatement.setBoolean(3, blacklist);

                collectionsStatement.executeUpdate();
            }

            @Cleanup val translationsStatement = connection
                    .prepareStatement("INSERT INTO `" + tablePrefix + "translations` (`collection`, `type`, " +
                            "`field_key`, `content`, `blacklist`, `servers`, `locations`, `patterns`, `twin_id`, " +
                            "`twin_data`, `archived`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
            for (Object o : items) {
                val item = (JSONObject) o;
                val type = item.optString("type", "text");
                val collectionName = item.optString("fileName");
                val collection = metadata.optJSONObject(collectionName);

                translationsStatement.setString(1, collectionName);
                translationsStatement.setString(2, type);
                translationsStatement.setString(3, item.optString("key"));
                if (type.equals("sign"))
                    translationsStatement.setString(4, item.optJSONObject("lines").toString());
                else
                    translationsStatement.setString(4, item.optJSONObject("languages").toString());

                val blacklist = item.optBoolean("blacklist", true);
                val servers = item.optJSONArray("servers");

                if (blacklist == collection.optBoolean("blacklist", true))
                    translationsStatement.setNull(5, Types.BOOLEAN);
                else
                    translationsStatement.setBoolean(5, blacklist);

                if (servers == null || JSONUtils.isArrayEqualsIgnoreOrder(servers, collection.optJSONArray("servers")))
                    translationsStatement.setNull(6, Types.VARCHAR);
                else
                    translationsStatement.setString(6, servers.toString());

                if (type.equals("sign"))
                    translationsStatement.setString(7, item.optJSONArray("locations").toString());
                else
                    translationsStatement.setNull(7, Types.VARCHAR);

                val patterns = item.optJSONArray("patterns");
                if (patterns != null)
                    translationsStatement.setString(8, patterns.toString());
                else
                    translationsStatement.setNull(8, Types.VARCHAR);

                var twin = item.optJSONObject("_twin");
                if (twin == null) twin = new JSONObject();

                translationsStatement.setString(9, twin.optString("id", UUID.randomUUID().toString()));
                translationsStatement.setString(10, JSONUtils.getObjectWithoutKeys(twin, "id", "archived").toString());

                val archived = item.optBoolean("archived", false) || twin.optBoolean("archived", false);
                translationsStatement.setBoolean(11, archived);

                translationsStatement.executeUpdate();
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
