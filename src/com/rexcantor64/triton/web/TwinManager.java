package com.rexcantor64.triton.web;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.plugin.PluginLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.UUID;

public class TwinManager {

    private final Triton main;

    private static final int TWIN_VERSION = 2;
    private static final String BASE_URL = "https://twin.rexcantor64.com";

    public TwinManager(Triton main) {
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
            data.put("mainLanguage", main.getLanguageManager().getMainLanguage().getName());

            boolean changed = false;
            JSONArray items = main.getLanguageConfig().getRaw();
            for (int i = 0; i < items.length(); i++) {
                JSONObject obj = items.optJSONObject(i);
                if (obj == null) continue;
                JSONObject twin = obj.optJSONObject("_twin");
                if (twin == null) {
                    twin = new JSONObject();
                    obj.put("_twin", twin);
                    changed = true;
                }
                if (!twin.has("id")) {
                    twin.put("id", UUID.randomUUID().toString());
                    changed = true;
                }
                if (!twin.has("dateCreated")) {
                    twin.put("dateCreated", System.currentTimeMillis());
                    changed = true;
                }
                if (!twin.has("dateUpdated")) {
                    twin.put("dateUpdated", System.currentTimeMillis());
                    changed = true;
                }
                if (!twin.has("tags") && obj.optJSONArray("tags") != null) {
                    twin.put("tags", obj.optJSONArray("tags"));
                    obj.remove("tags");
                    changed = true;
                }
                if (obj.optString("type").equals("sign")) {
                    if ((obj.optJSONArray("locations")) != null) {
                        JSONArray locs = obj.optJSONArray("locations");
                        for (int j = 0; j < locs.length(); j++) {
                            JSONObject loc = locs.optJSONObject(j);
                            if (loc == null) continue;
                            if (!loc.has("id")) {
                                loc.put("id", UUID.randomUUID().toString());
                                changed = true;
                            }
                        }
                    }
                }
            }

            if (changed) {
                main.getLanguageConfig().saveFromRaw(items);
                main.getLanguageConfig().setup(false);
                main.logDebug("Updated items to be able to upload to TWIN");
            }

            data.put("data", items);


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
