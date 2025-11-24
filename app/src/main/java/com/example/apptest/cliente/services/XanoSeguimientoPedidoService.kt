package com.example.apptest.cliente.services

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface XanoSeguimientoPedidoService {
    @GET("seguimiento_pedido/venta/{venta_id}")
    suspend fun obtenerPorVenta(@Path("venta_id") ventaId: Long): SeguimientoPedido

    @GET("seguimiento_pedido/{id}")
    suspend fun obtener(@Path("id") id: Long): SeguimientoPedido

    @POST("seguimiento_pedido")
    suspend fun crear(@Body body: SeguimientoPedidoCreate): SeguimientoPedido

    @PATCH("update_seguimiento_pedido")
    suspend fun actualizar(@Body body: SeguimientoPedidoUpdate): SeguimientoPedido
}

data class SeguimientoPedido(
    val id: Long,
    val created_at: Long?,
    val venta_id: Long,
    val estado_envio: String,
    val numero_seguimiento: String?,
    val fecha_estimada_entrega: String?
)

data class SeguimientoPedidoCreate(
    val venta_id: Long,
    val estado_envio: String,
    val numero_seguimiento: String?,
    val fecha_estimada_entrega: String?
)

data class SeguimientoPedidoUpdate(
    val seguimiento_pedido_id: Long,
    val venta_id: Long?,
    val estado_envio: String?,
    val numero_seguimiento: String?,
    val fecha_estimada_entrega: String?
)