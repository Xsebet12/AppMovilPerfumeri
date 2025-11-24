package com.example.apptest.user.models

/**
 * Formularios auxiliares para construir cuerpos de petición para Xano.
 */
data class XanoLoginForm(
    val correo: String,
    val contrasena: String
) {
    fun toBody(): Map<String, Any> = mapOf(
        // Los nombres de campos de login pueden variar; aquí usamos una convención genérica
        "email" to correo,
        "password" to contrasena
    )
}

data class XanoClienteRegistro(
    val primer_nombre: String,
    val segundo_nombre: String? = null,
    val apellido_paterno: String,
    val apellido_materno: String? = null,
    val email_contacto: String,
    val password: String,
    val comuna_id: Int,
    val nombre_calle: String,
    val numero_calle: Int,
    val telefono_contacto: String? = null,
    val tipo_cliente: String = "Detalle", // valores permitidos: Detalle|Vip|Mayorista
    val habilitado: Boolean = true, 
) {
    init {
        require(password.length >= 8) { "La contraseña debe tener al menos 8 caracteres" }
    }
    fun toBody(): Map<String, Any> {
        val mapa = mutableMapOf<String, Any>(
            "tipo_registro" to "cliente",
            "primer_nombre" to primer_nombre,
            "apellido_paterno" to apellido_paterno,
            "email_contacto" to email_contacto,
            "password" to password,
            "comuna_id" to comuna_id,
            "nombre_calle" to nombre_calle,
            "numero_calle" to numero_calle,
            "tipo_cliente" to tipo_cliente,
            "habilitado" to habilitado
        )
        segundo_nombre?.let { mapa["segundo_nombre"] = it }
        apellido_materno?.let { mapa["apellido_materno"] = it }
        telefono_contacto?.let { mapa["telefono_contacto"] = it }
        return mapa
    }

    companion object {
        fun desdeUi(
            nombres: String,
            apellidos: String,
            correo: String,
            password: String,
            direccion: String,
            comunaId: Int = 1,
            telefono: String? = null,
            tipoCliente: String = "Detalle",
        ): XanoClienteRegistro {
            val (primerNombre, segundoNombre) = descomponerNombres(nombres)
            val (apPat, apMat) = descomponerApellidos(apellidos)
            val (calle, numero) = descomponerDireccion(direccion)
            return XanoClienteRegistro(
                primer_nombre = primerNombre,
                segundo_nombre = segundoNombre,
                apellido_paterno = apPat,
                apellido_materno = apMat,
                email_contacto = correo,
                password = password,
                comuna_id = comunaId,
                nombre_calle = calle,
                numero_calle = numero ?: 1,
                telefono_contacto = telefono,
                tipo_cliente = tipoCliente,
                habilitado = true
            )
        }

        private fun descomponerNombres(nombres: String): Pair<String, String?> {
            val partes = nombres.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val primero = partes.getOrNull(0) ?: ""
            val segundo = partes.drop(1).joinToString(" ").ifBlank { null }
            return primero to segundo
        }

        private fun descomponerApellidos(apellidos: String): Pair<String, String?> {
            val partes = apellidos.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val paterno = partes.getOrNull(0) ?: ""
            val materno = partes.drop(1).joinToString(" ").ifBlank { null }
            return paterno to materno
        }

        private fun descomponerDireccion(direccion: String): Pair<String, Int?> {
            // Intenta extraer un número al final; si no hay, devuelve null para el número
            val texto = direccion.trim()
            val match = Regex("^(.*?)(\\s+(\\d+))$").find(texto)
            return if (match != null) {
                val calle = match.groupValues[1].trim()
                val numero = match.groupValues[3].toIntOrNull()
                calle to numero
            } else {
                texto to null
            }
        }
    }
}
