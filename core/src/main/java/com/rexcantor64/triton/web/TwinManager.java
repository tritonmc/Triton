package com.rexcantor64.triton.web;

import com.google.gson.*;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.language.item.TWINData;
import com.rexcantor64.triton.language.item.serializers.LanguageItemSerializer;
import com.rexcantor64.triton.language.item.serializers.LanguageSignSerializer;
import com.rexcantor64.triton.language.item.serializers.LanguageTextSerializer;
import com.rexcantor64.triton.plugin.PluginLoader;
import lombok.val;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TwinManager {

    static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LanguageItem.class, new LanguageItemSerializer())
            .registerTypeAdapter(LanguageText.class, new LanguageTextSerializer())
            .registerTypeAdapter(LanguageSign.class, new LanguageSignSerializer())
            .create();
    private static final int TWIN_VERSION = 6;
    private static final String BASE_URL = "https://twin.rexcantor64.com";
    private final Triton main;

    public TwinManager(Triton main) {
        this.main = main;
    }

    public HttpResponse upload() {
        return upload(null, null);
    }

    public HttpResponse upload(List<String> allowedCollections, List<String> allowedLanguages) {
        try {
            val bungee = Triton.isBungee();
            if (!bungee && main.getConf().isBungeecord())
                return null;

            val data = new JsonObject();

            data.addProperty("tritonv", TWIN_VERSION);
            data.addProperty("user", "%%__USER__%%");
            data.addProperty("resource", "%%__RESOURCE__%%-" + main.getVersion());
            data.addProperty("nonce", "%%__NONCE__%%");
            data.addProperty("bungee", bungee);

            if (allowedCollections != null || allowedLanguages != null) {
                val limit = new JsonObject();
                if (allowedCollections != null)
                    limit.add("collections", gson.toJsonTree(allowedCollections));
                if (allowedLanguages != null)
                    limit.add("languages", gson.toJsonTree(allowedLanguages));
                data.add("limit", limit);
            }

            val languages = new JsonArray();
            for (val lang : main.getLanguageManager().getAllLanguages())
                if (allowedLanguages == null || allowedLanguages.contains(lang.getName()))
                    languages.add(new JsonPrimitive(lang.getName()));
            data.add("languages", languages);
            data.addProperty("mainLanguage", main.getLanguageManager().getMainLanguage().getName());

            val changed = new ArrayList<LanguageItem>();

            val items = new JsonArray();
            val metadata = new JsonObject();
            for (val collection : main.getStorage().getCollections().entrySet()) {
                if (allowedCollections != null && !allowedCollections.contains(collection.getKey())) continue;
                for (val item : collection.getValue().getItems()) {
                    if (item.getTwinData() == null) item.setTwinData(new TWINData());
                    if (item.getTwinData().ensureValid()) changed.add(item);

                    val jsonItem = (JsonObject) gson.toJsonTree(item);
                    jsonItem.addProperty("fileName", collection.getKey());

                    if (allowedLanguages != null) {
                        try {
                            for (val key : new String[]{"languages", "lines"}) {
                                val obj = jsonItem.getAsJsonObject(key);
                                if (obj == null) continue;
                                obj.entrySet().removeIf(entry -> !allowedLanguages.contains(entry.getKey()));
                            }
                        } catch (Exception e) {
                            Triton.get().getLogger()
                                    .logError("Could not strip blocked languages from translation while uploading " +
                                            "to TWIN");
                            e.printStackTrace();
                        }
                    }

                    items.add(jsonItem);
                }
                if (bungee)
                    metadata.add(collection.getKey(), gson.toJsonTree(collection.getValue().getMetadata()));
            }

            if (changed.size() > 0) {
                main.getStorage().uploadPartiallyToStorage(main.getStorage().getCollections(), changed, null);
                main.getLogger().logInfo(2, "Updated items to be able to upload to TWIN");
            }

            data.add("data", items);
            if (bungee)
                data.add("metadata", metadata);

            val encodedData = data.toString();
            val u = new URL(BASE_URL + "/api/v1/upload");
            val conn = (HttpURLConnection) u.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Triton " + main.getConf().getTwinToken());
            val os = new DataOutputStream(conn.getOutputStream());
            os.write(encodedData.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            val responseCode = conn.getResponseCode();

            try {
                InputStream is;
                if (responseCode < HttpURLConnection.HTTP_BAD_REQUEST)
                    is = conn.getInputStream();
                else
                    is = conn.getErrorStream();

                val in = new BufferedReader(new InputStreamReader(is));

                String inputLine;
                val response = new StringBuilder();
                while ((inputLine = in.readLine()) != null)
                    response.append(inputLine);
                in.close();

                return new HttpResponse(true, responseCode, response.toString());
            } catch (FileNotFoundException e) {
                return new HttpResponse(true, responseCode, "");
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
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
            e.printStackTrace();
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
