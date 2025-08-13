import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.sql.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeatherForecastFetcher {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/weatheraqi";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Vruksha@2014";

    public static void main(String[] args) {
        String cityName = "Jaipur"; // From frontend
        String attributeName = "temperature_2m_max"; // From frontend

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            double[] latLon = getLatLon(conn, cityName);
            if (latLon == null) {
                System.out.println("City not found.");
                return;
            }

            String apiUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&daily=%s&timezone=auto",
                    latLon[0], latLon[1],
                    "weather_code,temperature_2m_max,apparent_temperature_max,apparent_temperature_min," +
                            "temperature_2m_min,sunrise,sunset,daylight_duration,sunshine_duration," +
                            "uv_index_max,rain_sum,precipitation_sum,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant"
            );

            String response = fetchAPI(apiUrl);

            printSpecificAttribute(response, attributeName);

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

    private static String fetchAPI(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private static void printSpecificAttribute(String json, String attributeName) {
        JsonObject dailyObj = JsonParser.parseString(json)
                .getAsJsonObject()
                .getAsJsonObject("daily");

        if (!dailyObj.has(attributeName)) {
            System.out.println("Invalid attribute: " + attributeName);
            return;
        }

        JsonArray dates = dailyObj.getAsJsonArray("time");
        JsonArray attributeValues = dailyObj.getAsJsonArray(attributeName);

        System.out.println("Forecast for attribute: " + attributeName);
        for (int i = 0; i < dates.size(); i++) {
            System.out.printf("%s - %s: %s%n",
                    dates.get(i).getAsString(),
                    attributeName,
                    attributeValues.get(i).getAsString()
            );
        }
    }
}
