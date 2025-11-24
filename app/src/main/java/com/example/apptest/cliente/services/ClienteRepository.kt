package com.example.apptest.cliente.services

import android.content.Context
import com.example.apptest.cliente.models.XanoCliente
import com.example.apptest.core.network.ApiClient
import com.google.gson.JsonObject
import com.example.apptest.cliente.services.XanoClienteService
import com.example.apptest.cliente.services.XanoClienteEditarService
import okhttp3.ResponseBody

class ClienteRepository(private val context: Context) {
    private val servicio by lazy { ApiClient.getRetrofit(context).create(XanoClienteService::class.java) }
    private val servicioEditar by lazy { ApiClient.getRetrofit(context).create(XanoClienteEditarService::class.java) }

    suspend fun listar(): Result<List<XanoCliente>> = runCatching { servicio.listar() }
    suspend fun obtener(id: Int): Result<XanoCliente> = runCatching { servicio.obtener(id) }
    suspend fun crear(datos: Map<String, Any?>): Result<XanoCliente> = runCatching { servicio.crear(datos) }
    suspend fun actualizar(id: Int, datos: Map<String, Any?>): Result<XanoCliente> = runCatching {
        val cuerpo = datos.toMutableMap().apply { put("cliente_id", id) }
        val res = servicioEditar.editar(cuerpo)
        // Tras editar perfil, re-obtener el cliente actualizado
        servicio.obtener(id)
    }
    suspend fun actualizarHabilitado(id: Int, habilitado: Boolean): Result<XanoCliente> = runCatching { servicio.actualizarEstado(id, mapOf("habilitado" to habilitado)) }
    suspend fun eliminar(id: Int): Result<String> = runCatching {
        val body: ResponseBody = servicio.eliminar(id)
        val raw = body.string().trim()
        val parsed = try {
            if (raw.startsWith("{")) {
                val json = com.google.gson.JsonParser.parseString(raw).asJsonObject
                json.get("message")?.asString ?: raw
            } else raw
        } catch (_: Exception) { raw }
        val cleaned = parsed.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "Cliente eliminado"
        cleaned
    }
}
