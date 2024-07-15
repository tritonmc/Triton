package com.rexcantor64.triton.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.language.item.TWINData;
import com.rexcantor64.triton.language.item.serializers.LanguageItemSerializer;
import com.rexcantor64.triton.language.item.serializers.LanguageSignSerializer;
import com.rexcantor64.triton.language.item.serializers.LanguageTextSerializer;
import com.rexcantor64.triton.web.exceptions.NotOnProxyException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class TwinManager {

    static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LanguageItem.class, new LanguageItemSerializer())
            .registerTypeAdapter(LanguageText.class, new LanguageTextSerializer())
            .registerTypeAdapter(LanguageSign.class, new LanguageSignSerializer())
            .create();
    private static final int TWIN_VERSION = 6;
    private final Triton<?, ?> main;

    public @NotNull HttpResponse upload(@Nullable List<String> allowedCollections, @Nullable List<String> allowedLanguages) throws NotOnProxyException {
        val isProxy = Triton.isProxy();
        if (!isProxy && main.getConfig().isBungeecord()) {
            throw new NotOnProxyException();
        }
        try {

            val data = new JsonObject();

            data.addProperty("tritonv", TWIN_VERSION);
            data.addProperty("user", "%%__USER__%%");
            data.addProperty("resource", "%%__RESOURCE__%%-" + main.getVersion());
            data.addProperty("nonce", "%%__NONCE__%%");
            data.addProperty("bungee", isProxy);

            if (allowedCollections != null || allowedLanguages != null) {
                val limit = new JsonObject();
                if (allowedCollections != null) {
                    limit.add("collections", gson.toJsonTree(allowedCollections));
                }
                if (allowedLanguages != null) {
                    limit.add("languages", gson.toJsonTree(allowedLanguages));
                }
                data.add("limit", limit);
            }

            val languages = new JsonArray();
            for (val lang : main.getLanguageManager().getAllLanguages()) {
                if (allowedLanguages == null || allowedLanguages.contains(lang.getName())) {
                    languages.add(new JsonPrimitive(lang.getName()));
                }
            }
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
                                    .logError(e, "Could not strip blocked languages from translation while uploading " +
                                            "to TWIN");
                        }
                    }

                    items.add(jsonItem);
                }
                if (isProxy) {
                    metadata.add(collection.getKey(), gson.toJsonTree(collection.getValue().getMetadata()));
                }
            }

            if (changed.size() > 0) {
                main.getStorage().uploadPartiallyToStorage(main.getStorage().getCollections(), changed, null);
                main.getLogger().logInfo("Updated items to be able to upload to TWIN");
            }

            data.add("data", items);
            if (isProxy) {
                data.add("metadata", metadata);
            }

            val encodedData = data.toString();
            val u = new URL(getBaseUrl() + "/api/v1/upload");
            val conn = (HttpURLConnection) u.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Triton " + main.getConfig().getTwinToken());
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

    public @NotNull HttpResponse download(@NotNull String id) throws NotOnProxyException {
        if (Triton.isSpigot() && main.getConfig().isBungeecord()) {
            throw new NotOnProxyException();
        }
        try {
            URL u = new URL(getBaseUrl() + "/api/v1/get/" + id);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Triton " + main.getConfig().getTwinToken());

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

    public static String getBaseUrl() {
        return Triton.get().getConfig().getTwinInstance();
    }

    @RequiredArgsConstructor
    @Getter
    public static class HttpResponse {
        private final boolean success;
        private final int statusCode;
        private final @NotNull String page;
    }

}
