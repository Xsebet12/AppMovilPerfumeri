package com.example.apptest.core.storage

import com.example.apptest.user.services.CatalogoCategoria
import com.example.apptest.user.services.CatalogoMarca

/**
 * Cache en memoria para categorías y marcas traídas desde `obtener_catalogo_completo`.
 * Evita múltiples llamadas cuando Create/Edit product necesitan listas.
 */
object CatalogCache {
    @Volatile private var categorias: List<CatalogoCategoria> = emptyList()
    @Volatile private var marcas: List<CatalogoMarca> = emptyList()
    @Volatile private var timestamp: Long = 0L

    fun set(cats: List<CatalogoCategoria>, marks: List<CatalogoMarca>) {
        categorias = cats
        marcas = marks
        timestamp = System.currentTimeMillis()
    }

    fun getCategorias(): List<CatalogoCategoria> = categorias
    fun getMarcas(): List<CatalogoMarca> = marcas

    fun isFresh(maxAgeMs: Long = 30_000): Boolean =
        categorias.isNotEmpty() && marcas.isNotEmpty() && (System.currentTimeMillis() - timestamp) < maxAgeMs
}
