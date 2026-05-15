package com.example.meteohelper.data.api

import com.google.gson.annotations.SerializedName

data class KpResponse(
    @SerializedName("estimated_planetary_kp")
    val kpValue: String,
    val time_tag: String
)