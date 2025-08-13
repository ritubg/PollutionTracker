package com.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CurrentAQIForCities {
    static final String DB_URL = "jdbc:mysql://localhost:3306/weatheraqi";
    static final String DB_USER = "root";
    static final String DB_PASS = "pass";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            Statement cityStmt = conn.createStatement();
            ResultSet cities = cityStmt.executeQuery("SELECT city_name, latitude, longitude FROM city");

            while (cities.next()) {
                String cityName = cities.getString("city_name");
                double latitude = cities.getDouble("latitude");
                double longitude = cities.getDouble("longitude");

                String aqiUrl = String.format(
                    "https://air-quality-api.open-meteo.com/v1/air-quality" +
                    "?latitude=%.4f&longitude=%.4f" +
                    "&current=pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone",
                    latitude, longitude
                );

                try {
                    String response = fetchAPI(aqiUrl);
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    JsonObject current = json.getAsJsonObject("current");

                    // API returns arrays for each parameter
                    double pm10 = getValue(current.get("pm10"));
double pm25 = getValue(current.get("pm2_5"));
double co   = getValue(current.get("carbon_monoxide"));
double no2  = getValue(current.get("nitrogen_dioxide"));
double so2  = getValue(current.get("sulphur_dioxide"));
double o3   = getValue(current.get("ozone"));


                    System.out.printf(
                        "\nCity: %s\nPM10: %.2f µg/m³\nPM2.5: %.2f µg/m³\nCO: %.2f µg/m³\nNO2: %.2f µg/m³\nSO2: %.2f µg/m³\nO3: %.2f µg/m³\n",
                        cityName, pm10, pm25, co, no2, so2, o3
                    );

                } catch (Exception e) {
                    System.err.println("Error fetching data for " + cityName + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String fetchAPI(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

   private static double getValue(JsonElement element) {
    if (element == null || element.isJsonNull()) {
        return Double.NaN;
    }
    if (element.isJsonArray()) {
        JsonArray arr = element.getAsJsonArray();
        return (arr.size() > 0 && !arr.get(0).isJsonNull()) ? arr.get(0).getAsDouble() : Double.NaN;
    }
    // If it's just a number
    if (element.isJsonPrimitive()) {
        return element.getAsDouble();
    }
    return Double.NaN;
}

}
