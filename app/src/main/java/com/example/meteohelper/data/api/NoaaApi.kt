package com.example.meteohelper.data.api

import retrofit2.http.GET

interface NoaaApi {
    @GET("json/planetary_k_index_1d.json")  // ← Полный путь от base URL
    suspend fun getKpIndex(): List<KpResponse>
}