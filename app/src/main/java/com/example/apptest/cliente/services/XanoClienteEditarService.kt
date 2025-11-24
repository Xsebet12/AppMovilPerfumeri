package com.example.apptest.cliente.services

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.PATCH

interface XanoClienteEditarService {
    @PATCH("editarPerfilCliente")
    suspend fun editar(@Body cuerpo: Map<String, @JvmSuppressWildcards Any?>): JsonObject
}
