package com.rexcantor64.triton.storage;

public class SQLManager {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    public SQLManager(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }



}
