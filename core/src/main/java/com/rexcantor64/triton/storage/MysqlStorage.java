package com.rexcantor64.triton.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.language.item.*;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.var;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class MysqlStorage extends Storage {

    private static final Type SIGN_TYPE = new TypeToken<HashMap<String, String[]>>() {
    }.getType();
    private static final Type TEXT_TYPE = new TypeToken<HashMap<String, String>>() {
    }.getType();
    private static final Type LOCATIONS_TYPE = new TypeToken<List<SignLocation>>() {
    }.getType();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();
    private static final Gson gson = new Gson();

    private final HikariConfig config = new HikariConfig();
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    private final String tablePrefix;
    private HikariDataSource dataSource;
    private IpCache ipCache;

    @Override
    public void load() {
        config.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL=false");
        config.setUsername(user);
        config.setPassword(password);

        val tritonConfig = Triton.get().getConfig();

        config.setMaximumPoolSize(tritonConfig.getDatabaseMysqlPoolMaxSize());
        config.setMinimumIdle(tritonConfig.getDatabaseMysqlPoolMinIdle());
        config.setMaxLifetime(tritonConfig.getDatabaseMysqlPoolMaxLifetime());
        config.setConnectionTimeout(tritonConfig.getDatabaseMysqlPoolConnTimeout());

        for (val entry : tritonConfig.getDatabaseMysqlPoolProperties().entrySet())
            config.addDataSourceProperty(entry.getKey(), entry.getValue());

        this.dataSource = new HikariDataSource(config);

        this.ipCache = new IpCache();

        if (!setup()) throw new RuntimeException("Failed to setup database connection");

        val data = downloadFromStorage();
        if (data == null) throw new RuntimeException("Failed to get translations from database");

        this.collections = data;
    }

    private Connection openConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    private boolean setup() {
        try (Connection connection = openConnection()) {
            Statement stmt = connection.createStatement();
            // TODO sanitize tablePrefix
            stmt.execute("CREATE TABLE IF NOT EXISTS `" + tablePrefix + "player_data` ( `key` VARCHAR(39) NOT NULL , " +
                    "`value` VARCHAR(100) NOT NULL, PRIMARY KEY (`key`) );");
            stmt.execute("CREATE TABLE IF NOT EXISTS `" + tablePrefix + "collections` ( `name` VARCHAR(100) NOT NULL " +
                    ", `servers` TEXT NOT NULL , `blacklist` BOOLEAN NOT NULL , PRIMARY KEY (`name`));");
            stmt.execute("CREATE TABLE IF NOT EXISTS `" + tablePrefix + "translations` ( `collection` VARCHAR(100) " +
                    "NOT NULL , `type` ENUM('text','sign') NOT NULL DEFAULT 'text' , `field_key` VARCHAR(200) NOT " +
                    "NULL , `content` MEDIUMTEXT NOT NULL , `blacklist` BOOLEAN NULL DEFAULT NULL , `servers` TEXT " +
                    "NULL DEFAULT NULL , `locations` MEDIUMTEXT NULL DEFAULT NULL , `patterns` TEXT NULL DEFAULT NULL" +
                    " , `twin_id` VARCHAR(36) NOT NULL , `twin_data` TEXT NOT NULL , UNIQUE (`twin_id`) , CONSTRAINT " +
                    "`collections_translations` FOREIGN KEY (`collection`) REFERENCES `triton_collections`(`name`) ON" +
                    " DELETE RESTRICT ON UPDATE CASCADE);");
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
        Triton.get().getLogger().logInfo(2, "Saving language for %1...", entity);
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
            Triton.get().getLogger().logInfo(2, "Saved!");
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
    public boolean uploadToStorage(ConcurrentHashMap<String, Collection> collections) {
        return uploadPartiallyToStorage(collections, null, null);
    }

    @Override
    public boolean uploadPartiallyToStorage(ConcurrentHashMap<String, Collection> collections,
                                            List<LanguageItem> changed, List<LanguageItem> deleted) {
        if (Triton.get().getConf().isBungeecord() && Triton.get() instanceof SpigotMLP) return true;

        try {
            @Cleanup val connection = openConnection();

            if (changed == null && deleted == null) {
                @Cleanup val emptyTablesStatement = connection.createStatement();
                emptyTablesStatement.execute("TRUNCATE `" + tablePrefix + "translations`");
                emptyTablesStatement.execute("DELETE FROM `" + tablePrefix + "collections`");
            }

            @Cleanup val collectionsStatement = connection
                    .prepareStatement("INSERT INTO `" + tablePrefix + "collections` (`name`, `servers`, `blacklist`) " +
                            "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `servers` = VALUES (`servers`), `blacklist` = " +
                            "VALUES(`blacklist`);");

            @Cleanup val translationsStatement = connection
                    .prepareStatement("INSERT INTO `" + tablePrefix + "translations` (`collection`, `type`, " +
                            "`field_key`, `content`, `blacklist`, `servers`, `locations`, `patterns`, `twin_id`, " +
                            "`twin_data`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `collection` " +
                            "= VALUES (`collection`), `type` = VALUES (`type`), `field_key` = VALUES (`field_key`), " +
                            "`content` = VALUES (`content`), `blacklist` = VALUES (`blacklist`), `servers` = VALUES " +
                            "(`servers`), `locations` = VALUES (`locations`), `patterns` = VALUES (`patterns`), " +
                            "`twin_data` = VALUES (`twin_data`);");

            @Cleanup val translationsDeleteStatement = connection
                    .prepareStatement("DELETE FROM `" + tablePrefix + "translations` WHERE `twin_id` = ?");

            if (deleted != null) {
                for (val item : deleted) {
                    if (item.getTwinData() == null || item.getTwinData().getId() == null) {
                        Triton.get().getLogger()
                                .logWarning("Failed to delete item %1 from database because it doesn't have a TWIN " +
                                        "id", item);
                        continue;
                    }

                    translationsDeleteStatement.setString(1, item.getTwinData().getId().toString());
                    translationsDeleteStatement.executeUpdate();
                }
            }

            for (val entry : collections.entrySet()) {
                collectionsStatement.setString(1, entry.getKey());

                val metadata = entry.getValue().getMetadata();

                collectionsStatement.setString(2, gson.toJson(metadata.getServers()));
                collectionsStatement.setBoolean(3, metadata.isBlacklist());

                collectionsStatement.executeUpdate();

                for (val item : entry.getValue().getItems()) {
                    // Ignore item if it hasn't changed
                    if (changed != null && !changed.contains(item)) continue;

                    val type = item.getType();

                    translationsStatement.setString(1, entry.getKey());
                    translationsStatement.setString(2, type.getName());
                    translationsStatement.setString(3, item.getKey());
                    if (item instanceof LanguageSign) {
                        val itemSign = (LanguageSign) item;
                        translationsStatement.setString(4, toJsonOrDefault(itemSign.getLines(), "{}"));
                        translationsStatement.setNull(5, Types.BOOLEAN);
                        translationsStatement.setNull(6, Types.VARCHAR);
                        translationsStatement.setString(7, toJsonOrDefault(itemSign.getLocations(), "[]"));
                        translationsStatement.setNull(8, Types.VARCHAR);
                    } else {
                        val itemText = (LanguageText) item;

                        translationsStatement.setString(4, toJsonOrDefault(itemText.getLanguages(), "{}"));

                        val blacklist = itemText.getBlacklist();
                        val servers = itemText.getServers();

                        if (blacklist == null)
                            translationsStatement.setNull(5, Types.BOOLEAN);
                        else
                            translationsStatement.setBoolean(5, blacklist);

                        if (servers == null)
                            translationsStatement.setNull(6, Types.VARCHAR);
                        else
                            translationsStatement.setString(6, gson.toJson(servers));

                        translationsStatement.setNull(7, Types.VARCHAR);

                        val patterns = itemText.getPatterns();
                        if (patterns != null)
                            translationsStatement.setString(8, gson.toJson(patterns));
                        else
                            translationsStatement.setNull(8, Types.VARCHAR);
                    }

                    var twin = item.getTwinData();
                    if (twin == null) twin = new TWINData();
                    twin.ensureValid();

                    translationsStatement.setString(9, twin.getId().toString());
                    val twinData = (JsonObject) gson.toJsonTree(twin, TWINData.class);
                    twinData.remove("id");
                    translationsStatement.setString(10, gson.toJson(twinData));

                    translationsStatement.executeUpdate();
                }
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public ConcurrentHashMap<String, Collection> downloadFromStorage() {
        try {
            val collections = new ConcurrentHashMap<String, Collection>();
            @Cleanup Connection connection = openConnection();

            @Cleanup val collectionsStatement = connection
                    .prepareStatement("SELECT name, servers, blacklist FROM `" + tablePrefix + "collections`;");

            @Cleanup val collectionsResult = collectionsStatement.executeQuery();

            while (collectionsResult.next()) {
                val col = new Collection();
                col.getMetadata().setServers(gson.fromJson(collectionsResult.getString("servers"), STRING_LIST_TYPE));
                col.getMetadata().setBlacklist(collectionsResult.getBoolean("blacklist"));
                collections.put(collectionsResult.getString("name"), col);
            }

            @Cleanup val translationsStatement = connection
                    .prepareStatement("SELECT collection, type, field_key, content, blacklist, servers, locations, " +
                            "patterns, twin_id, twin_data FROM `" + tablePrefix + "translations`;");

            @Cleanup val translationsResult = translationsStatement.executeQuery();

            while (translationsResult.next()) {
                val type = translationsResult.getString("type");
                if (type.equalsIgnoreCase("text")) {
                    val item = new LanguageText();

                    item.setKey(translationsResult.getString("field_key"));

                    item.setLanguages(gson.fromJson(translationsResult.getString("content"), TEXT_TYPE));
                    val blacklist = translationsResult.getObject("blacklist");
                    if (blacklist != null)
                        item.setBlacklist((boolean) blacklist);

                    val servers = translationsResult.getString("servers");
                    if (servers != null)
                        item.setServers(gson.fromJson(servers, STRING_LIST_TYPE));

                    val patterns = translationsResult.getString("patterns");
                    if (patterns != null)
                        item.setPatterns(gson.fromJson(patterns, STRING_LIST_TYPE));

                    val twinDataString = translationsResult.getString("twin_data");
                    val twinData = gson.fromJson(twinDataString, TWINData.class);

                    val twinId = translationsResult.getString("twin_id");
                    twinData.setId(UUID.fromString(twinId));

                    item.setTwinData(twinData);

                    val col = collections.get(translationsResult.getString("collection"));
                    if (col != null)
                        col.getItems().add(item);
                } else if (type.equalsIgnoreCase("sign")) {
                    val item = new LanguageSign();

                    item.setKey(translationsResult.getString("field_key"));

                    item.setLines(gson.fromJson(translationsResult.getString("content"), SIGN_TYPE));

                    val locations = translationsResult.getString("locations");
                    if (locations != null)
                        item.setLocations(gson.fromJson(locations, LOCATIONS_TYPE));

                    val twinDataString = translationsResult.getString("twin_data");
                    val twinData = gson.fromJson(twinDataString, TWINData.class);

                    val twinId = translationsResult.getString("twin_id");
                    twinData.setId(UUID.fromString(twinId));

                    item.setTwinData(twinData);

                    val col = collections.get(translationsResult.getString("collection"));
                    if (col != null)
                        col.getItems().add(item);
                }
            }

            return collections;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String toJsonOrDefault(Object obj, String def) {
        if (obj == null) return def;
        return gson.toJson(obj);
    }

    public String toString() {
        return "MySQL";
    }
}
