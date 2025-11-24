package com.example.apptest.cliente.services

import com.example.apptest.cliente.models.XanoCliente
import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.http.*

interface XanoClienteService {
    @GET("cliente")
    suspend fun listar(): List<XanoCliente>

    @GET("cliente/{id}")
    suspend fun obtener(@Path("id") id: Int): XanoCliente

    @POST("cliente")
    suspend fun crear(@Body cuerpo: Map<String, @JvmSuppressWildcards Any?>): XanoCliente

    @PATCH("cliente/{id}")
    suspend fun actualizar(@Path("id") id: Int, @Body cuerpo: Map<String, @JvmSuppressWildcards Any?>): XanoCliente

    @PUT("cliente/{id}")
    suspend fun actualizarEstado(@Path("id") id: Int, @Body cuerpo: Map<String, Boolean>): XanoCliente

    @DELETE("cliente/{id}")
    suspend fun eliminar(@Path("id") id: Int): ResponseBody
}
