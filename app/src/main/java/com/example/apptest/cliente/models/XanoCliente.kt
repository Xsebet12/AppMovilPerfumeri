package com.example.apptest.cliente.models

data class XanoCliente(
    val id: Int? = null,
    val created_at: String? = null,
    val primer_nombre: String? = null,
    val segundo_nombre: String? = null,
    val apellido_paterno: String? = null,
    val apellido_materno: String? = null,
    val nombre_calle: String? = null,
    val numero_calle: String? = null,
    val telefono_contacto: String? = null,
    val email_contacto: String? = null,
    val comuna_id: Int? = null,
    val tipo_cliente: String? = null,
    val password: String? = null,
    val habilitado: Boolean? = null
)
