package com.example.apptest.ui.empleado

import android.content.Context
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.apptest.cliente.services.ClienteRepository
import com.example.apptest.core.network.ApiClient
import com.example.apptest.pais.services.XanoRegComunaService
import com.example.apptest.cliente.models.XanoCliente
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserDetailState(
    val cliente: XanoCliente? = null,
    val cargando: Boolean = false,
    val error: String? = null,
    val regiones: List<Pair<Int,String>> = emptyList(),
    val comunas: List<Pair<Int,String>> = emptyList(),
    val comunasDetalladas: List<Triple<Int, String, Int?>> = emptyList(),
    val guardando: Boolean = false
)

class UserDetailViewModel(private val context: Context, private val clienteId: Int): ViewModel() {
    private val repo = ClienteRepository(context)
    private val regSrv by lazy { ApiClient.getRetrofit(context).create(XanoRegComunaService::class.java) }

    private val _state = MutableStateFlow(UserDetailState(cargando = true))
    val state: StateFlow<UserDetailState> = _state

    init {
        cargar()
        cargarRegionesComunas()
    }

    private fun cargar() {
        viewModelScope.launch {
            repo.obtener(clienteId).onSuccess { c ->
                _state.update { it.copy(cliente = c, cargando = false) }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message, cargando = false) }
            }
        }
    }

    private fun cargarRegionesComunas() {
        viewModelScope.launch {
            try {
                val regionesCompletas = regSrv.obtenerRegionesConComunas()
                val regiones = regionesCompletas.map { it.id to (it.nombreRegion ?: it.id.toString()) }
                val comunasDet = regionesCompletas.flatMap { r -> r.comunas.orEmpty().map { c -> Triple(c.id, (c.nombreComuna ?: c.id.toString()), c.regionId) } }
                val comunas = comunasDet.map { it.first to it.second }
                _state.update { it.copy(regiones = regiones, comunas = comunas, comunasDetalladas = comunasDet) }
            } catch (_: Exception) { }
        }
    }

    fun toggleHabilitado() {
        val id = _state.value.cliente?.id ?: return
        viewModelScope.launch {
            repo.actualizarHabilitado(id, _state.value.cliente?.habilitado != true).onSuccess { c ->
                _state.update { it.copy(cliente = c) }
            }
        }
    }

    fun guardarCambios(campos: Map<String, Any?>, onFin: (Boolean)->Unit) {
        val id = _state.value.cliente?.id ?: return
        _state.update { it.copy(guardando = true) }
        viewModelScope.launch {
            repo.actualizar(id, campos).onSuccess { c ->
                _state.update { it.copy(cliente = c, guardando = false) }
                onFin(true)
            }.onFailure { e ->
                _state.update { it.copy(error = e.message, guardando = false) }
                onFin(false)
            }
        }
    }

    fun eliminar(onFin: (Boolean, String?)->Unit) {
        val id = _state.value.cliente?.id
        if (id == null) {
            onFin(false, "ID nulo")
            return
        }
        viewModelScope.launch {
            repo.eliminar(id).onSuccess { msg ->
                onFin(true, msg)
            }.onFailure { e ->
                Log.e("UserDetailVM", "Error eliminando", e)
                onFin(false, e.message)
            }
        }
    }
}
