package com.example.apptest.core.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.apptest.user.models.User
import com.google.gson.Gson

class SessionManager private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile private var inMemoryToken: String? = null
    private val gson = Gson()

    fun saveToken(token: String) {
        inMemoryToken = token
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun setSessionOnlyToken(token: String) {
        inMemoryToken = token
    }

    fun getToken(): String? {
        return inMemoryToken ?: prefs.getString(KEY_TOKEN, null)
    }

    fun saveUser(user: User) {
        val json = gson.toJson(user)
        prefs.edit().putString(KEY_USER, json).apply()
    }

    /**
     * Guarda (o mezcla) solo user_type e id inmediatamente tras login, antes de descargar el perfil completo.
     * Si ya existe un User en prefs, se preservan sus otros campos.
     */
    fun saveMinimalUser(userId: Long?, userType: String?) {
        val existente = getUser()
        val nuevo = User(
            id = userId ?: existente?.id,
            nombres = existente?.nombres,
            apellidos = existente?.apellidos,
            rut = existente?.rut,
            dv = existente?.dv,
            correo = existente?.correo,
            rol = existente?.rol,
            direccion = existente?.direccion,
            enabled = existente?.enabled,
            createdAt = existente?.createdAt,
            user_type = userType ?: existente?.user_type,
            username = existente?.username,
            telefono_contacto = existente?.telefono_contacto,
            tipo_cliente = existente?.tipo_cliente,
            comuna_id = existente?.comuna_id,
            nombre_comuna = existente?.nombre_comuna,
            nombre_calle = existente?.nombre_calle,
            numero_calle = existente?.numero_calle,
            rol_id = existente?.rol_id
        )
        saveUser(nuevo)
    }

    fun getUser(): User? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getUserName(): String? {
        val user = getUser() ?: return null
        val nombres = user.nombres ?: ""
        val apellidos = user.apellidos ?: ""
        return listOf(nombres, apellidos).filter { it.isNotBlank() }.joinToString(" ").ifEmpty { null }
    }

    fun getUserEmail(): String? {
        return getUser()?.correo
    }

    fun clear() {
        inMemoryToken = null
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER).apply()
    }

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER = "user_json"

        @Volatile private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context).also { instance = it }
            }
        }
    }
}
