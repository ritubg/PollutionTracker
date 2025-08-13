import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.sql.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeatherAttributeFetcher {

    // DB connection config
    private static final String DB_URL = "jdbc:mysql://localhost:3306/weatheraqi";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Vruksha@2014";

    public static void main(String[] args) {
        String cityName = "New Delhi";      // Frontend input
        String attribute = "pm10"; // Frontend input

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Step 1: Get lat/lon from DB
            double[] latLon = getLatLon(conn, cityName);
            if (latLon == null) {
                System.out.println("City not found.");
                return;
            }

            // Step 2: Decide API type
            String apiUrl = getApiUrl(attribute, latLon[0], latLon[1]);

            if (apiUrl == null) {
                System.out.println("Attribute not recognized.");
                return;
            }

            // Step 3: Fetch API data
            String response = fetchAPI(apiUrl);

            // Step 4: Extract the specific attribute
            String value = extractAttribute(response, attribute);

            System.out.println(attribute + " for " + cityName + ": " + value);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double[] getLatLon(Connection conn, String cityName) throws SQLException {
        String sql = "SELECT latitude, longitude FROM city WHERE city_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cityName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new double[]{rs.getDouble("latitude"), rs.getDouble("longitude")};
            }
        }
        return null;
    }

    private static String getApiUrl(String attribute, double lat, double lon) {
        String weatherParams = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation," +
                "rain,weather_code,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m";
        String aqiParams = "pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,ozone,sulphur_dioxide";

        if (weatherParams.contains(attribute)) {
            return String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=%s&timezone=auto",
                    lat, lon, attribute);
        } else if (aqiParams.contains(attribute)) {
            return String.format(
                    "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=%.4f&longitude=%.4f&current=%s&timezone=auto",
                    lat, lon, attribute);
        }
        return null;
    }

    private static String fetchAPI(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private static String extractAttribute(String json, String attribute) {
        JsonObject currentObj = JsonParser.parseString(json)
                .getAsJsonObject()
                .getAsJsonObject("current");
        if (currentObj.has(attribute)) {
            return currentObj.get(attribute).toString();
        }
        return "N/A";
    }
}