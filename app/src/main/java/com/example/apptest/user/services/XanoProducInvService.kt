package com.example.apptest.user.services


import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.DELETE
import retrofit2.Response
    

interface XanoProducInvService {
    @GET("producInv")
    suspend fun listar(): List<XanoProducInvItem>

    @GET("producInv/{id}")
    suspend fun obtener(@Path("id") id: Long): XanoProducInvItem?

    // Crear producto + inventario + imágenes (auth=empleado)
    @Multipart
    @POST("producInv")
    suspend fun crearProducto(
        @Part("sku") sku: RequestBody,
        @Part("nombre_producto") nombreProducto: RequestBody,
        @Part("descripcion") descripcion: RequestBody?,
        @Part("ml") ml: RequestBody?,
        @Part("presentacion") presentacion: RequestBody?,
        @Part("categoria_id") categoriaId: RequestBody,
        @Part("marca_id") marcaId: RequestBody,
        @Part("stock") stock: RequestBody?,
        @Part("costo_referencia") costoReferencia: RequestBody,
        @Part("precio_detalle") precioDetalle: RequestBody,
        @Part("precio_vip") precioVip: RequestBody,
        @Part("precio_mayorista") precioMayorista: RequestBody,
        @Part imagenes: List<MultipartBody.Part>?,
        @Part("principal_image_index") indicePrincipal: RequestBody?
    ): XanoCrearProductoRespuesta

    // Modificar producto + inventario y agregar imágenes (auth=empleado)
    // PATCH simplificado (sin imágenes) usando JSON payload parcial
    @PATCH("producInv/{producto_id}")
    suspend fun editarProductoSimple(
        @Path("producto_id") productoId: Long,
        @Body body: XanoProductoPatchRequest
    ): XanoModificarProductoRespuesta

    // Actualizar estado rápido (habilitado / disponible / stock) sin tocar resto de campos
    @Multipart
    @POST("producInv/update")
    suspend fun actualizarEstadoProducto(
        @Part("producto_id") productoId: RequestBody,
        @Part("habilitado") habilitado: RequestBody?,
        @Part("disponible") disponible: RequestBody?,
        @Part("stock") stock: RequestBody?
    ): XanoModificarProductoRespuesta

    // Agregar imagen individual (separado de modificación global)
    @Multipart
    @POST("producto_imagen")
    suspend fun agregarImagenProducto(
        @Part("producto_id") productoId: RequestBody,
        @Part imagen: MultipartBody.Part,
        @Part("img_principal") imgPrincipal: RequestBody?
    ): Any

    // Eliminar imagen
    @DELETE("producto_imagen/{imagen_id}")
    suspend fun eliminarImagenProducto(@Path("imagen_id") imagenId: Long): Response<Unit>

    // Marcar imagen como principal
    @Multipart
    @PATCH("producto_imagen/{imagen_id}")
    suspend fun marcarImagenPrincipal(
        @Path("imagen_id") imagenId: Long,
        @Part("producto_id") productoId: RequestBody,
        @Part("img_principal") imgPrincipal: RequestBody
    ): Any

    // Nuevo endpoint simplificado para establecer principal
    @POST("producto_imagen/set_principal")
    suspend fun setImagenPrincipal(@Body body: XanoSetPrincipalRequest): ProducInvAdminImagen?

    // Eliminar producto completo requiere enviar product_id en body según API
    @HTTP(method = "DELETE", path = "producInv/{producto_id}", hasBody = true)
    suspend fun eliminarProducto(
        @Path("producto_id") productoId: Long,
        @Body body: XanoEliminarProductoBody
    ): XanoEliminarProductoRespuesta
}

// Endpoint catálogo completo (productos, marcas, categorías) para reducir llamadas.
interface XanoCatalogoCompletoService {
    // Catálogo completo para administración: productos + marcas + categorías
    @GET("ProducInvAdmin")
    suspend fun obtener(): CatalogoCompletoRespuesta
}

// Detalle completo admin (incluye campos avanzados si el backend los aporta)
interface XanoProducInvAdminService {
    @GET("ProducInvAdmin/{id}")
    suspend fun obtener(@Path("id") id: Long): ProducInvAdminDetalle?
}

// Modelo detalle admin
data class ProducInvAdminDetalle(
    val id: Long?,
    val sku: String?,
    val nombre: String?,
    val descripcion: String?,
    val categoria: String?,
    @SerializedName("categoria_id") val categoria_id: Long?,
    val marca: String?,
    @SerializedName("marca_id") val marca_id: Long?,
    val ml: Int?,
    val presentacion: String?,
    val habilitado: Boolean?,
    val stock: Int?,
    val precio_detalle: Double?,
    val precio_vip: Double?,
    val precio_mayorista: Double?,
    val costo_referencia: Double?,
    val disponible: Boolean?,
    val imagenes: List<ProducInvAdminImagen>? = emptyList(),
    val marcas: List<CatalogoMarca>? = emptyList(),
    val categorias: List<CatalogoCategoria>? = emptyList()
)

data class ProducInvAdminImagen(
    val id: Long?,
    val created_at: Long?,
    val producto_id: Long?,
    val img_principal: Boolean?,
    @SerializedName("url_imagen") val url_imagen: CatalogoImagen?
)

data class CatalogoCompletoRespuesta(
    val productos: List<CatalogoProducto> = emptyList(),
    val marcas: List<CatalogoMarca> = emptyList(),
    val categorias: List<CatalogoCategoria> = emptyList()
)

data class CatalogoProducto(
    @SerializedName("id_producto") val id_producto: Long?,
    @SerializedName("id_inventario") val id_inventario: Long?,
    val sku: String,
    @SerializedName("nombre") val nombre: String,
    val descripcion: String?,
    val ml: Int?,
    val presentacion: String?,
    @SerializedName("categoria_id") val categoria_id: Long?,
    @SerializedName("marca_id") val marca_id: Long?,
    @SerializedName("nom_marca") val nom_marca: String?,
    @SerializedName("nom_categoria") val nom_categoria: String?,
    val habilitado: Boolean?,
    val stock: Int?,
    @SerializedName("costo") val costo: Double?,
    val precio_detalle: Double?,
    val precio_vip: Double?,
    val precio_mayorista: Double?,
    val disponible: Boolean?,
    @SerializedName("url_imagen") val url_imagen: CatalogoImagen? // puede ser null
)

data class CatalogoImagen(
    val access: String?,
    val path: String?,
    val name: String?,
    val type: String?,
    val size: Long?,
    val mime: String?,
    val meta: CatalogoImagenMeta?,
    val url: String?
)

data class CatalogoImagenMeta(
    val width: Int?,
    val height: Int?
)

data class CatalogoMarca(
    val id: Long?,
    val nombre_marca: String?
)

data class CatalogoCategoria(
    val id: Long?,
    val nombre_categoria: String?
)

// Servicios simples para categoría y marca
interface XanoCategoriaService {
    @GET("categoria")
    suspend fun listar(): List<XanoCategoria>
}

interface XanoMarcaService {
    @GET("marca")
    suspend fun listar(): List<XanoMarca>
}

data class XanoCategoria(
    val id: Long?,
    @SerializedName("nombre_categoria") val nombre_categoria: String?
)

data class XanoMarca(
    val id: Long?,
    @SerializedName("nombre_marca") val nombre_marca: String?
)

    data class XanoProducInvItem(
        val id: Long,
        @SerializedName(value = "id_inventario", alternate = ["inventario_id"]) val inventario_id: Long?,
        val created_at: Long?,
        val sku: String?,
        val nombre_producto: String,
        val descripcion: String?,
        val ml: Int?,
    val presentacion: String?,
    val categoria_id: Long?,
    val marca_id: Long?,
    @SerializedName("categoria_nombre") val nombre_categoria: String?,
    @SerializedName("marca_nombre") val nombre_marca: String?,
    val stock: Int?,
    val disponible: Boolean?,
    @SerializedName("habilitado") val habilitado: Boolean?,
    val precio_detalle: Double?,
    val precio_vip: Double?,
    val precio_mayorista: Double?,
    val costo_referencia: Double?,
    @SerializedName(value = "url_imagen_principal", alternate = ["imagen_url"]) val url_imagen_principal: String?,
    @SerializedName("url_imagen_extras") val url_imagen_extras: List<String> = emptyList()
)

data class XanoCrearProductoRespuesta(
    val message: String?,
    @SerializedName("product_id") val productId: Long?,
    @SerializedName("inventory_id") val inventoryId: Long?,
    val images: List<Map<String, Any?>>?
)

data class XanoModificarProductoRespuesta(
    val producto: Any?,
    val inventario: Any?,
    val imagenes: List<Any?>?
)

data class XanoProductoPatchRequest(
    val producto_id: Long, // redundante con path, requerido por backend
    val sku: String? = null,
    val nombre_producto: String? = null,
    val descripcion: String? = null,
    val ml: Int? = null,
    val presentacion: String? = null,
    val categoria_id: Long? = null,
    val marca_id: Long? = null,
    val habilitado: Boolean? = null,
    val disponible: Boolean? = null,
    val stock: Int? = null,
    val costo_referencia: Double? = null,
    val precio_detalle: Double? = null,
    val precio_vip: Double? = null,
    val precio_mayorista: Double? = null
)

data class XanoEliminarProductoBody(
    val product_id: Long
)

data class XanoEliminarProductoRespuesta(
    val success: Boolean?,
    val message: String?
)

data class XanoSetPrincipalRequest(val id: Long)