package com.rexcantor64.triton.storage;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.player.LanguagePlayer;

import java.sql.*;
import java.util.UUID;

public class MysqlStorage implements PlayerStorage {

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

        this.ipCache = new IpCache();
    }

    private Connection openConnection() throws SQLException, ClassNotFoundException {
        synchronized (this) {
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager
                    .getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL" +
                            "=false", this.user, this.password);
        }
    }

    public boolean setup() {
        try (Connection connection = openConnection()) {
            Statement stmt = connection.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS `" + tablePrefix + "player_data` ( `key` VARCHAR(39) NOT NULL , " +
                    "`value` " +
                    "VARCHAR(100) NOT NULL, PRIMARY KEY (`key`) );");
            stmt.close();
            return true;
        } catch (SQLException e) {
            Triton.get().logError("Error connecting to database: %1", e.getMessage());
        } catch (ClassNotFoundException e) {
            Triton.get().logError("Failed to find mysql driver!");
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
        Triton.get().logDebug("Saving language for %1...", entity);
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
            Triton.get().logDebug("Saved!");
        } catch (Exception e) {
            e.printStackTrace();
            Triton.get().logError("Failed to save language for %1! Could not insert into database: %2", entity, e
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
        } catch (SQLException | ClassNotFoundException e) {
            Triton.get().logError("Failed to get value from the database: %1", e.getMessage());
        }
        return result;
    }

}
