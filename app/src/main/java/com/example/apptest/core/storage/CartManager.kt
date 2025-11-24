package com.example.apptest.core.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Gestor ligero de carrito usando SharedPreferences (sin MVVM).
 * Guarda lista de items serializada en JSON por usuario.
 */
class CartManager private constructor(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("cart_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        @Volatile private var instancia: CartManager? = null
        fun getInstance(context: Context): CartManager =
            instancia ?: synchronized(this) { instancia ?: CartManager(context.applicationContext).also { instancia = it } }
    }

    fun obtenerItems(idUsuario: Long?): List<ItemCarrito> {
        if (idUsuario == null) return emptyList()
        val json = prefs.getString(claveUsuario(idUsuario), null) ?: return emptyList()
        return try {
            val tipo = object: TypeToken<List<ItemCarrito>>(){}.type
            gson.fromJson(json, tipo) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun guardarItems(idUsuario: Long?, items: List<ItemCarrito>) {
        if (idUsuario == null) return
        val json = gson.toJson(items)
        prefs.edit().putString(claveUsuario(idUsuario), json).apply()
    }

    fun agregarItem(idUsuario: Long?, nuevo: ItemCarrito) {
        if (idUsuario == null) return
        val lista = obtenerItems(idUsuario).toMutableList()
        val existenteIndex = lista.indexOfFirst { it.inventario_id == nuevo.inventario_id }
        if (existenteIndex >= 0) {
            val existente = lista[existenteIndex]
            lista[existenteIndex] = existente.copy(cantidad = existente.cantidad + nuevo.cantidad)
        } else lista.add(nuevo)
        guardarItems(idUsuario, lista)
    }

    fun actualizarCantidad(idUsuario: Long?, inventarioId: Long, nuevaCantidad: Int) {
        if (idUsuario == null) return
        val lista = obtenerItems(idUsuario).toMutableList()
        val idx = lista.indexOfFirst { it.inventario_id == inventarioId }
        if (idx >= 0) {
            if (nuevaCantidad <= 0) lista.removeAt(idx) else lista[idx] = lista[idx].copy(cantidad = nuevaCantidad)
            guardarItems(idUsuario, lista)
        }
    }

    fun eliminarItem(idUsuario: Long?, inventarioId: Long) {
        if (idUsuario == null) return
        val lista = obtenerItems(idUsuario).filter { it.inventario_id != inventarioId }
        guardarItems(idUsuario, lista)
    }

    fun limpiar(idUsuario: Long?) { if (idUsuario != null) prefs.edit().remove(claveUsuario(idUsuario)).apply() }

    private fun claveUsuario(id: Long) = "carrito_usuario_$id"
}


data class ItemCarrito(
    val inventario_id: Long,
    val producto_id: Long?,
    val nombre_producto: String?,
    val cantidad: Int,
    val precio_unitario: Double, 
    val costo_referencia: Double?,
    val stock_disponible: Int?
) {
    val subtotal: Double get() = cantidad * precio_unitario
}
