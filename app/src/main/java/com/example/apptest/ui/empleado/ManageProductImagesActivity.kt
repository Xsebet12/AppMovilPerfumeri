package com.example.apptest.ui.empleado

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apptest.user.services.XanoProducInvService
import com.example.apptest.user.services.XanoProducInvAdminService
import com.example.apptest.user.services.ProducInvAdminImagen
import com.example.apptest.user.services.XanoSetPrincipalRequest
import com.example.apptest.core.network.ApiClient
import com.example.apptest.databinding.ActivityManageProductImagesBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ManageProductImagesActivity : ComponentActivity() {
    private lateinit var binding: ActivityManageProductImagesBinding
    private var productId: Long = -1L
    private val adapter = ProductImageAdapter(
        onMarkPrincipal = { img -> marcarPrincipal(img) },
        onDelete = { img -> eliminarImagen(img) }
    )

    private val selectorImagenes = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isEmpty()) return@registerForActivityResult
        // Si sólo se selecciona una, usar el valor del checkbox para principal
        if (uris.size == 1) {
            subirImagen(uris.first(), binding.chkNuevaPrincipal.isChecked)
        } else {
            // Para múltiples, subir todas como no-principales y pedir luego marcar una.
            uris.forEach { subirImagen(it, false) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityManageProductImagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productId = intent.getLongExtra("product_id", -1L)
        if (productId == -1L) { finish(); return }

        binding.recyclerImagenes.layoutManager = LinearLayoutManager(this)
        binding.recyclerImagenes.adapter = adapter

        binding.btnSubirImagen.setOnClickListener { selectorImagenes.launch(arrayOf("image/*")) }
        binding.btnRefrescar.setOnClickListener { cargarDetalle() }

        cargarDetalle()
    }

    private fun cargarDetalle() {
        val retrofit = ApiClient.getRetrofit(this)
        val adminSrv = retrofit.create(XanoProducInvAdminService::class.java)
        lifecycleScope.launch {
            try {
                val det = adminSrv.obtener(productId)
                val lista = det?.imagenes ?: emptyList()
                adapter.submitList(lista)
                binding.tvEstado.text = "Total imágenes: ${lista.size}"
            } catch (e: Exception) {
                Toast.makeText(this@ManageProductImagesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun subirImagen(uri: Uri, principal: Boolean) {
        val retrofit = ApiClient.getRetrofit(this)
        val servicio = retrofit.create(XanoProducInvService::class.java)
        lifecycleScope.launch {
            try {
                val archivo = copiarUriATmp(uri)
                val tipo = contentResolver.getType(uri)?.toMediaTypeOrNull() ?: "image/*".toMediaTypeOrNull()
                val cuerpo = archivo.asRequestBody(tipo)
                val part = MultipartBody.Part.createFormData("imagen", archivo.name, cuerpo)
                val rbProd = productId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val rbPrincipal = if (principal) "true".toRequestBody("text/plain".toMediaTypeOrNull()) else null
                servicio.agregarImagenProducto(rbProd, part, rbPrincipal)
                Toast.makeText(this@ManageProductImagesActivity, "Imagen subida", Toast.LENGTH_SHORT).show()
                cargarDetalle()
            } catch (e: Exception) {
                Toast.makeText(this@ManageProductImagesActivity, "Error subiendo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun marcarPrincipal(img: ProducInvAdminImagen) {
        val retrofit = ApiClient.getRetrofit(this)
        val servicio = retrofit.create(XanoProducInvService::class.java)
        lifecycleScope.launch {
            try {
                val resp = servicio.setImagenPrincipal(XanoSetPrincipalRequest(img.id ?: return@launch))
                if (resp?.img_principal == true) {
                    Toast.makeText(this@ManageProductImagesActivity, "Marcada como principal", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ManageProductImagesActivity, "Respuesta inesperada", Toast.LENGTH_LONG).show()
                }
                cargarDetalle()
            } catch (e: Exception) {
                Toast.makeText(this@ManageProductImagesActivity, "Error marcando: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun eliminarImagen(img: ProducInvAdminImagen) {
        val retrofit = ApiClient.getRetrofit(this)
        val servicio = retrofit.create(XanoProducInvService::class.java)
        lifecycleScope.launch {
            try {
                val resp = servicio.eliminarImagenProducto(img.id ?: return@launch)
                if (resp.isSuccessful) {
                    Toast.makeText(this@ManageProductImagesActivity, "Imagen eliminada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ManageProductImagesActivity, "Error eliminando: ${resp.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageProductImagesActivity, "Error eliminando: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                cargarDetalle()
            }
        }
    }

    private fun copiarUriATmp(uri: Uri): File {
        val mime = contentResolver.getType(uri) ?: ""
        val ext = when (mime.lowercase()) {
            "image/jpeg", "image/jpg" -> ".jpg"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "image/gif" -> ".gif"
            else -> ".img"
        }
        val baseNombre = (uri.lastPathSegment ?: "imagen").substringAfterLast('/')
        val nombreFinal = if (baseNombre.contains('.')) baseNombre else baseNombre + ext
        val tmp = File.createTempFile("upload_", "_${nombreFinal}", cacheDir)
        contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(tmp).use { output ->
                if (input != null) input.copyTo(output)
            }
        }
        return tmp
    }
}
