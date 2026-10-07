package com.example.meteohelper.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")  // ← Теперь "v1/forecast", так как base URL = "https://api.open-meteo.com/"
    suspend fun getWeather(
        @Query("latitude") lat: Double = 55.75,
        @Query("longitude") lon: Double = 37.62,
        @Query("hourly") hourly: String = "temperature_2m,relativehumidity_2m,pressure_msl",
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("timezone") timezone: String = "Europe/Moscow"
    ): WeatherResponse
}