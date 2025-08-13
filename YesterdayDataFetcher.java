import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.LocalDate;

public class YesterdayDataFetcher {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/weatheraqi";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Vruksha@2014";

    public static void main(String[] args) {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            Statement stmt = conn.createStatement();
            ResultSet cities = stmt.executeQuery("SELECT latitude, longitude FROM city");

            while (cities.next()) {
                double lat = cities.getDouble("latitude");
                double lon = cities.getDouble("longitude");

                String weatherUrl = String.format(
                        "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                                "&daily=temperature_2m_max,weather_code,temperature_2m_min,apparent_temperature_max,apparent_temperature_min," +
                                "sunset,sunrise,daylight_duration,sunshine_duration,rain_sum,precipitation_sum,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant" +
                                "&timezone=auto&start_date=%s&end_date=%s",
                        lat, lon, yesterday, yesterday
                );

                String aqiUrl = String.format(
                        "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=%.4f&longitude=%.4f" +
                                "&hourly=pm2_5,pm10,carbon_monoxide,carbon_dioxide,nitrogen_dioxide,sulphur_dioxide,ozone,methane" +
                                "&timezone=auto&start_date=%s&end_date=%s",
                        lat, lon, yesterday, yesterday
                );

                JsonObject weatherData = JsonParser.parseString(fetchAPI(weatherUrl))
                        .getAsJsonObject().getAsJsonObject("daily");
                JsonObject aqiData = JsonParser.parseString(fetchAPI(aqiUrl))
                        .getAsJsonObject().getAsJsonObject("hourly");

                insertData(conn, lat, lon, yesterday, weatherData, aqiData);
            }

            System.out.println("Yesterday's weather + AQI inserted successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertData(Connection conn, double lat, double lon, LocalDate date,
                            JsonObject weather, JsonObject aqi) throws SQLException {

        String sql = "INSERT INTO historicaldata VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, lat);
            ps.setDouble(2, lon);
            ps.setDate(3, Date.valueOf(date));
            ps.setInt(4, getInt(weather, "weather_code"));
            ps.setBigDecimal(5, getDecimal(weather, "temperature_2m_max"));
            ps.setBigDecimal(6, getDecimal(weather, "temperature_2m_min"));
            ps.setBigDecimal(7, getDecimal(weather, "apparent_temperature_max"));
            ps.setBigDecimal(8, getDecimal(weather, "apparent_temperature_min"));

            ps.setTime(9, Time.valueOf(getTime(weather, "sunrise")));
            ps.setTime(10, Time.valueOf(getTime(weather, "sunset")));

            ps.setInt(11, getInt(weather, "daylight_duration"));
            ps.setInt(12, getInt(weather, "sunshine_duration"));
            ps.setBigDecimal(13, getDecimal(weather, "wind_gusts_10m_max"));
            ps.setBigDecimal(14, getDecimal(weather, "wind_speed_10m_max"));
            ps.setBigDecimal(15, getDecimal(weather, "wind_direction_10m_dominant"));
            ps.setBigDecimal(16, getDecimal(weather, "rain_sum"));
            ps.setBigDecimal(17, getDecimal(weather, "precipitation_sum"));
            ps.setNull(18, Types.DECIMAL);
            ps.setNull(19, Types.DECIMAL);

            double[] pm25 = getMinMax(aqi.getAsJsonArray("pm2_5"));
            double[] pm10 = getMinMax(aqi.getAsJsonArray("pm10"));
            double[] co = getMinMax(aqi.getAsJsonArray("carbon_monoxide"));
            double[] no2 = getMinMax(aqi.getAsJsonArray("nitrogen_dioxide"));
            double[] so2 = getMinMax(aqi.getAsJsonArray("sulphur_dioxide"));
            double[] o3 = getMinMax(aqi.getAsJsonArray("ozone"));
            double[] co2 = getMinMax(aqi.getAsJsonArray("carbon_dioxide"));
            double[] ch4 = getMinMax(aqi.getAsJsonArray("methane"));

            setNullableDouble(ps, 20, pm25[0]); setNullableDouble(ps, 21, pm25[1]);
            setNullableDouble(ps, 22, pm10[0]); setNullableDouble(ps, 23, pm10[1]);
            setNullableDouble(ps, 24, co[0]);   setNullableDouble(ps, 25, co[1]);
            setNullableDouble(ps, 26, no2[0]);  setNullableDouble(ps, 27, no2[1]);
            setNullableDouble(ps, 28, so2[0]);  setNullableDouble(ps, 29, so2[1]);
            setNullableDouble(ps, 30, o3[0]);   setNullableDouble(ps, 31, o3[1]);
            setNullableDouble(ps, 32, co2[0]);  setNullableDouble(ps, 33, co2[1]);
            setNullableDouble(ps, 34, ch4[0]);  setNullableDouble(ps, 35, ch4[1]);

            ps.executeUpdate();
        }
    }

    private static String fetchAPI(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private static double[] getMinMax(JsonArray arr) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (int i = 0; i < arr.size(); i++) {
            if (!arr.get(i).isJsonNull()) {
                double val = arr.get(i).getAsDouble();
                min = Math.min(min, val);
                max = Math.max(max, val);
            }
        }
        return (min == Double.MAX_VALUE) ? new double[]{Double.NaN, Double.NaN} : new double[]{min, max};
    }

    private static void setNullableDouble(PreparedStatement ps, int idx, double val) throws SQLException {
        if (Double.isNaN(val) || Double.isInfinite(val)) ps.setNull(idx, Types.DOUBLE);
        else ps.setDouble(idx, val);
    }

    private static java.math.BigDecimal getDecimal(JsonObject obj, String key) {
        JsonArray arr = obj.getAsJsonArray(key);
        if (arr != null && arr.size() > 0 && !arr.get(0).isJsonNull()) {
            return arr.get(0).getAsBigDecimal();
        }
        return null;
    }

    private static int getInt(JsonObject obj, String key) {
        JsonArray arr = obj.getAsJsonArray(key);
        return (arr != null && arr.size() > 0 && !arr.get(0).isJsonNull()) ? arr.get(0).getAsInt() : 0;
    }

    private static String getTime(JsonObject obj, String key) {
        JsonArray arr = obj.getAsJsonArray(key);
        if (arr != null && arr.size() > 0 && !arr.get(0).isJsonNull()) {
            return arr.get(0).getAsString().substring(11) + ":00";
        }
        return "00:00:00";
    }
}
