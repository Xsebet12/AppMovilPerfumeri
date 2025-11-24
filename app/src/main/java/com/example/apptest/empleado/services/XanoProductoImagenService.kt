package com.example.apptest.empleado.services

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface XanoProductoImagenService {
    // Mantiene solo subida de imagen. Listado se obtiene en endpoint combinado.
    @Multipart
    @POST("producto_imagen")
    suspend fun subirImagen(
        @Part("producto_id") productoId: RequestBody,
        @Part imagen: MultipartBody.Part,
        @Part("img_principal") imgPrincipal: RequestBody? = null
    ): XanoProductoImagen
}

data class XanoProductoImagen(
    val id: Long?,
    val producto_id: Long?,
    val url_imagen: JsonObject?,
    val img_principal: Boolean?
)
