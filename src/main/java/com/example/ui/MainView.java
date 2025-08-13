package com.example.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

@Route("")
public class MainView extends VerticalLayout {

    private final String DB_URL = "jdbc:mysql://localhost:3306/weatheraqi";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "pass";

    public MainView() {

    H1 title = new H1("Weather & AQI Dashboard");
Span dateTime = new Span();
dateTime.getStyle().set("font-size", "14px");

HorizontalLayout topBar = new HorizontalLayout();
topBar.setWidthFull();
topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
topBar.add(title, dateTime);

add(topBar);

// Enable Vaadin polling (every second)
UI.getCurrent().setPollInterval(1000);

// Update the span on each poll
UI.getCurrent().addPollListener(e -> {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    dateTime.setText(LocalDateTime.now().format(formatter));
});

        // Left: Calendar (3/4)
        VerticalLayout leftLayout = new VerticalLayout();
        leftLayout.setSizeFull();
        DatePicker calendar = new DatePicker("Select Date");
        calendar.setWidthFull();
        calendar.setValue(LocalDate.now());
        leftLayout.add(calendar);

        // Right: AQI Data (1/4)
        VerticalLayout rightLayout = new VerticalLayout();
        rightLayout.setSizeFull();

        ComboBox<String> citySelector = new ComboBox<>("Select City");
        citySelector.setWidthFull();
        citySelector.setItems(fetchCityNamesFromDB());

        Grid<CityAQI> grid = new Grid<>(CityAQI.class, false);
        grid.addColumn(CityAQI::getCity).setHeader("City");
        grid.addColumn(CityAQI::getPm10).setHeader("PM10");
        grid.addColumn(CityAQI::getPm25).setHeader("PM2.5");
        grid.addColumn(CityAQI::getCo).setHeader("CO");
        grid.addColumn(CityAQI::getNo2).setHeader("NO2");
        grid.addColumn(CityAQI::getSo2).setHeader("SO2");
        grid.addColumn(CityAQI::getO3).setHeader("O3");

        Button refresh = new Button("Fetch", event -> {
            String city = citySelector.getValue();
            LocalDate date = calendar.getValue();
            if (city != null && !city.isEmpty() && date != null) {
                if (date.isEqual(LocalDate.now())) {
                    // Live data
                    grid.setItems(fetchLiveAQIDataForCity(city));
                } else {
                    // Historical data
                    showHistoricalData(city, date);
                }
            }
        });

        leftLayout.add(calendar);
        rightLayout.add(citySelector,refresh,grid);
        rightLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.add(leftLayout, rightLayout);
        mainLayout.setFlexGrow(3, leftLayout);
        mainLayout.setFlexGrow(1, rightLayout);

        add(mainLayout);
        setSizeFull();
    }

    private List<String> fetchCityNamesFromDB() {
        List<String> cities = new ArrayList<>();
        String query = "SELECT city_name FROM city ORDER BY city_name";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                cities.add(rs.getString("city_name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cities;
    }

    private List<CityAQI> fetchLiveAQIDataForCity(String city) {
        List<CityAQI> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT latitude, longitude FROM city WHERE city_name=?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, city);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    double lat = rs.getDouble("latitude");
                    double lon = rs.getDouble("longitude");
                    String apiUrl = String.format(
                        "https://air-quality-api.open-meteo.com/v1/air-quality" +
                        "?latitude=%.4f&longitude=%.4f" +
                        "&current=pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone",
                        lat, lon
                    );
                    CityAQI cityAQI = fetchAQIFromAPI(city, apiUrl);
                    list.add(cityAQI);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void showHistoricalData(String city, LocalDate date) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT * FROM city c JOIN historicaldata h " +
                    "ON c.latitude = h.latitude AND c.longitude = h.longitude " +
                    "WHERE c.city_name=? AND h.date=?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, city);
                ps.setDate(2, java.sql.Date.valueOf(date));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Dialog dialog = new Dialog();
                    dialog.setWidth("400px");
                    dialog.setHeight("500px");

                    StringBuilder sb = new StringBuilder();
                    sb.append("City: ").append(city).append("\n")
                      .append("Date: ").append(date).append("\n\n")
                      .append("PM10: ").append(rs.getDouble("pm10_max")).append("\n")
                      .append("PM2.5: ").append(rs.getDouble("pm25_max")).append("\n")
                      .append("CO: ").append(rs.getDouble("co_max")).append("\n")
                      .append("NO2: ").append(rs.getDouble("no2_max")).append("\n")
                      .append("SO2: ").append(rs.getDouble("so2_max")).append("\n")
                      .append("O3: ").append(rs.getDouble("o3_max")).append("\n");

                    dialog.add(new Paragraph(sb.toString()));
                    dialog.open();
                } else {
                    Dialog dialog = new Dialog();
                    dialog.add(new Paragraph("No historical data available for " + city + " on " + date));
                    dialog.open();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private CityAQI fetchAQIFromAPI(String city, String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            String response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonObject current = json.getAsJsonObject("current");

            double pm10 = getValue(current.get("pm10"));
            double pm25 = getValue(current.get("pm2_5"));
            double co   = getValue(current.get("carbon_monoxide"));
            double no2  = getValue(current.get("nitrogen_dioxide"));
            double so2  = getValue(current.get("sulphur_dioxide"));
            double o3   = getValue(current.get("ozone"));

            return new CityAQI(city, pm10, pm25, co, no2, so2, o3);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return new CityAQI(city, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }
    }

    private double getValue(JsonElement element) {
        if (element == null || element.isJsonNull()) return Double.NaN;
        if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            return (arr.size() > 0 && !arr.get(0).isJsonNull()) ? arr.get(0).getAsDouble() : Double.NaN;
        }
        if (element.isJsonPrimitive()) return element.getAsDouble();
        return Double.NaN;
    }
}
