package com.example.apptest.ui.empleado

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.apptest.databinding.ActivityCreateProductBinding
import com.example.apptest.user.services.XanoProducInvService
import com.example.apptest.user.services.XanoCatalogoCompletoService
import com.example.apptest.user.services.CatalogoCategoria
import com.example.apptest.user.services.CatalogoMarca
import com.example.apptest.core.storage.CatalogCache
import com.example.apptest.core.network.ApiClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * Pantalla simple para crear producto (Admin). No usa MVVM.
 * Campos mínimos: nombre, descripción, precio, stock, categoría (placeholder), imágenes opcionales.
 * Resto de campos se derivan o se completan con valores por defecto.
 */
class CreateProductActivity : ComponentActivity() {
    private lateinit var binding: ActivityCreateProductBinding
    private val imagenesSeleccionadas = mutableListOf<Uri>()
    private var indicePrincipal: Int = -1
    private var categorias: List<CatalogoCategoria> = emptyList()
    private var marcas: List<CatalogoMarca> = emptyList()

    private val selectorImagenes = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        imagenesSeleccionadas.clear()
        imagenesSeleccionadas.addAll(uris)
        Toast.makeText(this, "${uris.size} imágenes seleccionadas", Toast.LENGTH_SHORT).show()
        poblarSpinnerPrincipal()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreateProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarListasCatMarca()

        binding.btnPickImages.setOnClickListener {
            selectorImagenes.launch(arrayOf("image/*"))
        }

        binding.btnElegirPrincipal.setOnClickListener {
            if (imagenesSeleccionadas.isEmpty()) {
                Toast.makeText(this, "Seleccione imágenes primero", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            // Avanza índice principal cíclicamente
            indicePrincipal = if (indicePrincipal == -1) 0 else (indicePrincipal + 1) % imagenesSeleccionadas.size
            binding.tvPrincipalInfo.text = "Principal: Imagen ${indicePrincipal + 1}"
        }

        binding.btnSubmit.setOnClickListener { enviarCreacion() }

        binding.spPrincipal.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (imagenesSeleccionadas.isNotEmpty()) {
                    indicePrincipal = position
                    binding.tvPrincipalInfo.text = "Principal: Imagen ${position + 1}"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun enviarCreacion() {
        val sku = binding.etSku.text.toString().trim()
        val nombre = binding.etName.text.toString().trim()
        val descripcion = binding.etDescription.text.toString().trim().ifBlank { null }
        val mlVal = binding.etMl.text.toString().trim().toIntOrNull()
        val presentacionVal = binding.etPresentacion.text.toString().trim().ifBlank { null }
        val stockVal = binding.etStock.text.toString().trim().toIntOrNull()
        val costoRefVal = binding.etCostoReferencia.text.toString().trim().toDoubleOrNull()
        val precioDetalleVal = binding.etPrecioDetalle.text.toString().trim().toDoubleOrNull()
        val precioVipVal = binding.etPrecioVip.text.toString().trim().toDoubleOrNull()
        val precioMayoristaVal = binding.etPrecioMayorista.text.toString().trim().toDoubleOrNull()

        if (sku.isBlank()) { binding.etSku.error = "Obligatorio"; return }
        if (nombre.isBlank()) { binding.etName.error = "Obligatorio"; return }
        if (precioDetalleVal == null) { binding.etPrecioDetalle.error = "Obligatorio"; return }
        if (precioVipVal == null) { binding.etPrecioVip.error = "Obligatorio"; return }
        if (precioMayoristaVal == null) { binding.etPrecioMayorista.error = "Obligatorio"; return }
        if (categorias.isEmpty() || marcas.isEmpty()) { Toast.makeText(this, "Cat/Marca no cargadas", Toast.LENGTH_SHORT).show(); return }

        val categoriaIdSel = categorias.getOrNull(binding.spCategory.selectedItemPosition)?.id ?: 0L
        val marcaIdSel = marcas.getOrNull(binding.spBrand.selectedItemPosition)?.id ?: 0L

        val servicio = ApiClient.getRetrofit(this).create(XanoProducInvService::class.java)
        lifecycleScope.launch {
            try {
                val rb = {
                    s: String -> s.toRequestBody("text/plain".toMediaTypeOrNull())
                }
                val rbOpt = { v: Any? -> v?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()) }

                val partesImagenes = if (imagenesSeleccionadas.isNotEmpty()) {
                    imagenesSeleccionadas.mapIndexed { idx, uri ->
                        val archivo = copiarUriATmp(uri)
                        val tipo = contentResolver.getType(uri)?.toMediaTypeOrNull() ?: "image/*".toMediaTypeOrNull()
                        val cuerpo = archivo.asRequestBody(tipo)
                        MultipartBody.Part.createFormData("imagenes[$idx]", archivo.name, cuerpo)
                    }
                } else null

                val resp = servicio.crearProducto(
                    sku = rb(sku),
                    nombreProducto = rb(nombre),
                    descripcion = rbOpt(descripcion),
                    ml = rbOpt(mlVal),
                    presentacion = rbOpt(presentacionVal),
                    categoriaId = rb(categoriaIdSel.toString()),
                    marcaId = rb(marcaIdSel.toString()),
                    stock = rbOpt(stockVal),
                    costoReferencia = rb(costoRefVal?.toString() ?: precioDetalleVal.toString()),
                    precioDetalle = rb(precioDetalleVal.toString()),
                    precioVip = rb(precioVipVal.toString()),
                    precioMayorista = rb(precioMayoristaVal.toString()),
                    imagenes = partesImagenes,
                    indicePrincipal = rbOpt(if (indicePrincipal >= 0) indicePrincipal else null)
                )
                Toast.makeText(this@CreateProductActivity, resp.message ?: "Creado", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@CreateProductActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun cargarListasCatMarca() {
        val retrofit = ApiClient.getRetrofit(this)
        val catalogoSrv = retrofit.create(XanoCatalogoCompletoService::class.java)
        binding.spCategory.isEnabled = false
        binding.spBrand.isEnabled = false
        lifecycleScope.launch {
            try {
                if (!CatalogCache.isFresh()) {
                    val catalogo = catalogoSrv.obtener()
                    CatalogCache.set(catalogo.categorias, catalogo.marcas)
                }
                categorias = CatalogCache.getCategorias()
                marcas = CatalogCache.getMarcas()
                val nombresCat = categorias.map { it.nombre_categoria ?: "(Sin nombre)" }
                val nombresMarca = marcas.map { it.nombre_marca ?: "(Sin nombre)" }
                binding.spCategory.adapter = ArrayAdapter(this@CreateProductActivity, android.R.layout.simple_spinner_dropdown_item, nombresCat)
                binding.spBrand.adapter = ArrayAdapter(this@CreateProductActivity, android.R.layout.simple_spinner_dropdown_item, nombresMarca)
            } catch (e: Exception) {
                Toast.makeText(this@CreateProductActivity, "Error cargando catálogo: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.spCategory.isEnabled = true
                binding.spBrand.isEnabled = true
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
        val tmp = File.createTempFile("subida_", "_${nombreFinal}", cacheDir)
        contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(tmp).use { output ->
                if (input != null) input.copyTo(output)
            }
        }
        return tmp
    }

    private fun poblarSpinnerPrincipal() {
        if (imagenesSeleccionadas.isEmpty()) {
            binding.spPrincipal.adapter = null
            binding.tvPrincipalInfo.text = "Principal: (ninguna)"
            indicePrincipal = -1
            return
        }
        val etiquetas = imagenesSeleccionadas.indices.map { "Imagen ${it + 1}" }
        binding.spPrincipal.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, etiquetas)
        // Si ya había un principal previamente, mantén selección dentro de rango
        val idx = if (indicePrincipal in imagenesSeleccionadas.indices) indicePrincipal else 0
        binding.spPrincipal.setSelection(idx)
        indicePrincipal = idx
        binding.tvPrincipalInfo.text = "Principal: Imagen ${idx + 1}"
    }
}
