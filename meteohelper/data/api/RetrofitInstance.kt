package com.example.meteohelper.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val OPEN_METEO_BASE_URL = "https://api.open-meteo.com/"
    private const val NOAA_BASE_URL = "https://services.swpc.noaa.gov/"

    // ✅ Создаём OkHttpClient с увеличенными таймаутами
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // Было 10, стало 30
        .readTimeout(30, TimeUnit.SECONDS)     // Было 10, стало 30
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val weatherApi: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl(OPEN_METEO_BASE_URL)
            .client(okHttpClient)  // ← Добавляем наш клиент
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoApi::class.java)
    }

    val noaaApi: NoaaApi by lazy {
        Retrofit.Builder()
            .baseUrl(NOAA_BASE_URL)
            .client(okHttpClient)  // ← Добавляем наш клиент
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NoaaApi::class.java)
    }
}