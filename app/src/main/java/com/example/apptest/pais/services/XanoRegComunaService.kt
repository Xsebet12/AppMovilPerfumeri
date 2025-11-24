package com.example.apptest.pais.services

import com.example.apptest.pais.models.XanoRegionComuna
import retrofit2.http.GET

interface XanoRegComunaService {
    @GET("regComuna")
    suspend fun obtenerRegionesConComunas(): List<XanoRegionComuna>
}
