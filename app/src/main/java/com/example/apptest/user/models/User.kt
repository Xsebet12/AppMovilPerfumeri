package com.example.apptest.user.models

import com.google.gson.annotations.SerializedName

data class User(
    val id: Long?,
    val nombres: String?,
    val apellidos: String?,
    val rut: String?,
    val dv: String?,
    @SerializedName("correo") val correo: String?,
    val rol: String?,
    val direccion: String?,
    val enabled: Boolean?,
    val createdAt: String?,
    // Campos adicionales expuestos por Xano (opcionales)
    val user_type: String? = null,
    val username: String? = null,
    val telefono_contacto: String? = null,
    val tipo_cliente: String? = null,
    val comuna_id: Int? = null,
    val nombre_comuna: String? = null,
    val nombre_calle: String? = null,
    val numero_calle: Int? = null,
    val rol_id: Int? = null
)

