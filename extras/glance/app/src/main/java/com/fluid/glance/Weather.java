package com.fluid.glance;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import javax.net.ssl.HttpsURLConnection;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

final class Weather {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final long FRESH = 30 * 60_000L;
    private static final long EXPIRE = 6 * 60 * 60_000L;
    private static boolean running;
    private static String lastAttemptCity = "";
    private static long lastAttempt;

    static void refresh(Context source, Runnable updated) {
        Context context = source.getApplicationContext();
        SharedPreferences p = Glance.prefs(context);
        String city = p.getString("city", "").trim();
        if (city.isEmpty() || !p.getBoolean("weather", true)) return;
        synchronized (Weather.class) {
            long now = System.currentTimeMillis();
            if (running || (city.equals(p.getString("cachedCity", "")) && now - p.getLong("weatherAt", 0) < FRESH)
                    || (city.equals(lastAttemptCity) && now - lastAttempt < 3 * 60_000L)) return;
            running = true;
            lastAttemptCity = city;
            lastAttempt = now;
        }
        IO.execute(() -> {
            String status = "Weather unavailable";
            try {
                double lat, lon;
                if (city.equals(p.getString("resolvedCity", ""))) {
                    lat = Double.parseDouble(p.getString("latitude", ""));
                    lon = Double.parseDouble(p.getString("longitude", ""));
                } else {
                    JSONObject places = get("https://geocoding-api.open-meteo.com/v1/search?count=1&language=en&name="
                            + URLEncoder.encode(city, StandardCharsets.UTF_8.name()));
                    if (!places.has("results")) {
                        status = "City not found";
                        throw new IllegalArgumentException(status);
                    }
                    JSONObject place = places.getJSONArray("results").getJSONObject(0);
                    lat = place.getDouble("latitude");
                    lon = place.getDouble("longitude");
                    if (city.equals(p.getString("city", ""))) p.edit().putString("resolvedCity", city)
                            .putString("latitude", Double.toString(lat)).putString("longitude", Double.toString(lon)).apply();
                }
                JSONObject current = get("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                        + "&current=temperature_2m,weather_code").getJSONObject("current");
                if (city.equals(p.getString("city", ""))) p.edit().putString("cachedCity", city)
                        .putFloat("temp", (float)current.getDouble("temperature_2m")).putInt("code", current.getInt("weather_code"))
                        .putLong("weatherAt", System.currentTimeMillis()).remove("errorCity").apply();
            } catch (Exception e) {
                if (city.equals(p.getString("city", ""))) p.edit().putString("errorCity", city).putString("weatherError", status).apply();
            } finally {
                synchronized (Weather.class) { running = false; }
                updated.run();
            }
        });
    }

    static String value(SharedPreferences p) {
        String city = p.getString("city", "");
        if (city.isEmpty()) return "Set a city";
        long age = System.currentTimeMillis() - p.getLong("weatherAt", 0);
        if (!city.equals(p.getString("cachedCity", "")) || age > EXPIRE)
            return city.equals(p.getString("errorCity", "")) ? p.getString("weatherError", "") : "Updating…";
        boolean fahrenheit = p.getInt("units", 0) == 1;
        float c = p.getFloat("temp", 0);
        String result = Math.round(fahrenheit ? c * 1.8f + 32 : c) + (fahrenheit ? "°F" : "°C");
        if (p.getInt("weatherFormat", 0) == 0) result += " · " + description(p.getInt("code", -1));
        return age > 2 * 60 * 60_000L ? result + " *" : result;
    }

    private static String description(int code) {
        if (code == 0) return "Clear";
        if (code == 1 || code == 2) return "Partly cloudy";
        if (code == 3) return "Overcast";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "Snow";
        if (code >= 95) return "Thunder";
        return code >= 61 && code <= 82 ? "Rain" : "Conditions unavailable";
    }

    private static JSONObject get(String url) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        try (java.io.InputStream stream = connection.getInputStream()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int size;
            while ((size = stream.read(buffer)) != -1) {
                bytes.write(buffer, 0, size);
                if (bytes.size() > 128_000) throw new java.io.IOException("Response too large");
            }
            return new JSONObject(bytes.toString("UTF-8"));
        } finally { connection.disconnect(); }
    }
}
