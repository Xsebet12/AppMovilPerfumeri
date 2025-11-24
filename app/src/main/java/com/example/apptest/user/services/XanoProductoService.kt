package com.example.apptest.user.services

import retrofit2.http.GET
import retrofit2.http.Path

interface XanoProductoService {
    @GET("producto")
    suspend fun listar(): List<XanoProducto>

    @GET("producto/{id}")
    suspend fun obtener(@Path("id") id: Long): XanoProducto
}

data class XanoProducto(
    val id: Long,
    val created_at: Long?,
    val sku: String?,
    val nombre_producto: String,
    val descripcion: String?,
    val ml: Int?,
    val presentacion: String?,
    val categoria_id: Long?,
    val marca_id: Long?
)
