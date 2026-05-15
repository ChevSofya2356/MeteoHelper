package com.example.meteohelper.data.api

data class WeatherResponse(
    val current_weather: CurrentWeather,
    val hourly: Hourly,
    val hourly_units: HourlyUnits? = null  // Добавил nullable на случай изменений
)

data class CurrentWeather(
    val temperature: Float,
    val windspeed: Float,
    val weathercode: Int,
    val time: String  // ← ВАЖНО: "2026-05-13T14:00"
)

data class Hourly(
    val time: List<String>,
    val temperature_2m: List<Float>,
    val relativehumidity_2m: List<Int>,
    val pressure_msl: List<Float>
)

data class HourlyUnits(
    val temperature_2m: String? = null,
    val relativehumidity_2m: String? = null,
    val pressure_msl: String? = null
)