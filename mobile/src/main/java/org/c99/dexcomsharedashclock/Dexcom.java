package org.c99.dexcomsharedashclock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by sam on 11/15/15.
 */
public class Dexcom {
    public final static String APP_ID = "d89443d2-327c-4a6f-89e5-496bbb0317db";
    public final static String USER_AGENT = "Dexcom Share/3.0.2.11 CFNetwork/711.2.23 Darwin/14.0.0";

    public String login(String username, String password) throws IOException {
        try {
            HashMap<String, String>headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            JSONObject body = new JSONObject();
            body.put("accountName", username);
            body.put("password", password);
            body.put("applicationId", APP_ID);

            return fetch(new URL("https://share1.dexcom.com/ShareWebServices/Services/General/LoginPublisherAccountByName"), body.toString(), headers);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONArray latestGlucoseValues(String sessionId, int minutes, int maxCount) throws IOException {
        try {
            HashMap<String, String>headers = new HashMap<>();
            headers.put("Content-Type","application/json");
            String response = fetch(new URL("https://share1.dexcom.com/ShareWebServices/Services/Publisher/ReadPublisherLatestGlucoseValues" +
                    "?sessionId=" + sessionId +
                    "&minutes=" + minutes +
                    "&maxCount=" + maxCount), "", headers);
            return new JSONArray(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String fetch(URL url, String postdata, HashMap<String, String>headers) throws Exception {
        HttpURLConnection conn = null;

        if (url.getProtocol().toLowerCase().equals("https")) {
            conn = (HttpsURLConnection) url.openConnection(Proxy.NO_PROXY);
        } else {
            conn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        }

        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setUseCaches(false);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "application/json");
        if(headers != null) {
            for (String key : headers.keySet()) {
                conn.setRequestProperty(key, headers.get(key));
            }
        }

        if (postdata != null) {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            if(headers == null || !headers.containsKey("Content-Type"))
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            OutputStream ostr = null;
            try {
                ostr = conn.getOutputStream();
                ostr.write(postdata.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (ostr != null)
                    ostr.close();
            }
        }
        BufferedReader reader = null;
        String response = "";

        try {
            if (conn.getInputStream() != null) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()), 512);
            }
        } catch (IOException e) {
            if (conn.getErrorStream() != null) {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()), 512);
            }
        }

        if (reader != null) {
            response = toString(reader);
            reader.close();
        }
        conn.disconnect();
        return response;
    }

    private static String toString(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
