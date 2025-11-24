package com.example.apptest.pais.models

import com.google.gson.annotations.SerializedName

data class XanoRegionComuna(
    val id: Int,
    val created_at: Long?,
    @SerializedName("nombre_region") val nombreRegion: String?,
    val comunas: List<XanoComuna>?
)

data class XanoComuna(
    val id: Int,
    val created_at: Long?,
    @SerializedName("nombre_comuna") val nombreComuna: String?,
    @SerializedName("region_id") val regionId: Int?
)
