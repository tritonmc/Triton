package com.rexcantor64.triton.web;

import com.rexcantor64.triton.MultiLanguagePlugin;
import com.rexcantor64.triton.language.Language;
import com.rexcantor64.triton.plugin.PluginLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class TwinManager {

    private final MultiLanguagePlugin main;

    private static final int TWIN_VERSION = 1;
    private static final String BASE_URL = "https://twin.rexcantor64.com";

    public TwinManager(MultiLanguagePlugin main) {
        this.main = main;
    }

    public HttpResponse upload() {
        try {
            if (main.getLoader().getType() != PluginLoader.PluginType.BUNGEE && main.getConf().isBungeecord())
                return null;

            JSONObject data = new JSONObject();
            data.put("tritonv", TWIN_VERSION);
            data.put("bungee", main.getLoader().getType() == PluginLoader.PluginType.BUNGEE);
            JSONArray languages = new JSONArray();
            for (Language lang : main.getLanguageManager().getAllLanguages())
                languages.put(lang.getName());
            data.put("languages", languages);
            data.put("data", main.getLanguageConfig().getRaw());


            String encodedData = data.toString();
            URL u = new URL(BASE_URL + "/api/v1/upload");
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Triton " + main.getConf().getTwinToken());
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.write(encodedData.getBytes(Charset.defaultCharset()));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();

            try {
                InputStream is;
                if (responseCode < HttpURLConnection.HTTP_BAD_REQUEST)
                    is = conn.getInputStream();
                else
                    is = conn.getErrorStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return new HttpResponse(true, responseCode, response.toString());
            } catch (FileNotFoundException e) {
                return new HttpResponse(true, responseCode, "");
            }
        } catch (Exception e) {
            return new HttpResponse(false, 0, e.getMessage());
        }
    }

    public HttpResponse download(String id) {
        try {
            if (main.getLoader().getType() != PluginLoader.PluginType.BUNGEE && main.getConf().isBungeecord())
                return null;

            URL u = new URL(BASE_URL + "/api/v1/get/" + id);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Triton " + main.getConf().getTwinToken());

            int responseCode = conn.getResponseCode();

            try {
                InputStream is;
                if (responseCode < HttpURLConnection.HTTP_BAD_REQUEST)
                    is = conn.getInputStream();
                else
                    is = conn.getErrorStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return new HttpResponse(true, responseCode, response.toString());
            } catch (FileNotFoundException e) {
                return new HttpResponse(true, responseCode, "{}");
            }
        } catch (Exception e) {
            return new HttpResponse(false, 0, e.getMessage());
        }
    }

    public static class HttpResponse {

        private final boolean success;
        private final int statusCode;
        private final String page;

        private HttpResponse(boolean success, int statusCode, String page) {
            this.success = success;
            this.statusCode = statusCode;
            this.page = page;
        }

        public String getPage() {
            return page;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

}
