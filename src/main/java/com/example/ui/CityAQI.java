package com.example.ui;

public class CityAQI {
    private String city;
    private double pm10, pm25, co, no2, so2, o3;

    public CityAQI(String city, double pm10, double pm25, double co, double no2, double so2, double o3) {
        this.city = city;
        this.pm10 = pm10;
        this.pm25 = pm25;
        this.co = co;
        this.no2 = no2;
        this.so2 = so2;
        this.o3 = o3;
    }

    public String getCity() { return city; }
    public double getPm10() { return pm10; }
    public double getPm25() { return pm25; }
    public double getCo() { return co; }
    public double getNo2() { return no2; }
    public double getSo2() { return so2; }
    public double getO3() { return o3; }
}
