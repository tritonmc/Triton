package com.rexcantor64.triton.web;

import com.rexcantor64.triton.MultiLanguagePlugin;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class GistManager {

    private final MultiLanguagePlugin main;

    public GistManager(MultiLanguagePlugin main) {
        this.main = main;
    }

    public HttpResponse upload() {
        try {
            JSONObject data = new JSONObject();
            data.put("description", "Configuration from a Minecraft Server running MultiLanguagePlugin");
            data.put("public", false);
            JSONObject files = new JSONObject();
            JSONObject languageFile = new JSONObject();
            languageFile.put("content", main.getLanguageConfig().getRaw().toString());
            files.put("language.json", languageFile);
            JSONObject configFile = new JSONObject();
            configFile.put("content", main.getConf().toJSON().toString());
            files.put("config.json", configFile);
            data.put("files", files);
            String type = "application/json";
            String encodedData = data.toString();
            URL u = new URL("https://api.github.com/gists");
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", type);
            conn.setRequestProperty("Content-Length", String.valueOf(encodedData.length()));
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(encodedData);
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
                return new HttpResponse(true, responseCode, "{}");
            }
        } catch (Exception e) {
            return new HttpResponse(false, 0, e.getMessage());
        }
    }

    public HttpResponse downloader(String id) {
        try {
            URL u = new URL("https://api.github.com/gists/" + id);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

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
