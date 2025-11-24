package com.example.apptest.user.services

import retrofit2.http.GET

/**
 * Servicio para obtener inventarios y precios por tipo de cliente.
 * Endpoint base: api:cGjNNLgz/inventario
 */
interface XanoInventarioService {
    @GET("inventario")
    suspend fun listar(): List<XanoInventario>
}

data class XanoInventario(
    val id: Long,
    val created_at: Long?,
    val disponible: Boolean?,
    val stock: Int?,
    val costo_referencia: Double?,
    val precio_detalle: Double?,
    val precio_vip: Double?,
    val precio_mayorista: Double?,
    val producto_id: Long?
)
