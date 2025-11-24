package com.example.apptest.ui.empleado

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.apptest.user.services.XanoProducInvService
import com.example.apptest.user.services.XanoProducInvAdminService
import com.example.apptest.user.services.ProducInvAdminDetalle
import com.example.apptest.user.services.XanoCatalogoCompletoService
import com.example.apptest.user.services.CatalogoCategoria
import com.example.apptest.user.services.CatalogoMarca
import com.example.apptest.user.services.CatalogoProducto
import com.example.apptest.core.storage.CatalogCache
import com.example.apptest.core.network.ApiClient
import com.example.apptest.user.services.XanoProductoPatchRequest
import com.example.apptest.user.services.XanoEliminarProductoBody
import com.example.apptest.databinding.ActivityCreateProductBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileOutputStream


class EditProductActivity : ComponentActivity() {
    private lateinit var binding: ActivityCreateProductBinding
    private var productoId: Long = -1L
    private var categorias: List<CatalogoCategoria> = emptyList()
    private var marcas: List<CatalogoMarca> = emptyList()

    // Gestión de imágenes removida de edición; se hará en pantalla separada

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreateProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productoId = intent.getLongExtra("product_id", -1L)
        if (productoId == -1L) { finish(); return }

        binding.btnSubmit.text = "Guardar cambios"
        binding.btnPickImages.visibility = android.view.View.GONE
        binding.btnElegirPrincipal.visibility = android.view.View.GONE
        binding.spPrincipal.visibility = android.view.View.GONE
        binding.tvPrincipalInfo.visibility = android.view.View.GONE
        binding.tvLabelImagenes.visibility = android.view.View.GONE
        binding.btnSubmit.text = "Guardar cambios"
        // Botón eliminar visible en edición
        binding.btnEliminarProducto.visibility = android.view.View.VISIBLE
        binding.tvTituloCrear.text = "Editar Producto"

        cargarDatosProductoYListas()

        binding.btnSubmit.setOnClickListener { enviarPatch() }
        binding.btnEliminarProducto.setOnClickListener { eliminarProducto() }
    }

    private fun enviarPatch() {
        val nombre = binding.etName.text.toString().trim().ifBlank { null }
        val descripcion = binding.etDescription.text.toString().trim().ifBlank { null }
        val mlVal = binding.etMl.text.toString().trim().toIntOrNull()
        val presentacionVal = binding.etPresentacion.text.toString().trim().ifBlank { null }
        val stockVal = binding.etStock.text.toString().trim().toIntOrNull()
        val costoRefVal = binding.etCostoReferencia.text.toString().trim().toDoubleOrNull()
        val precioDetalleVal = binding.etPrecioDetalle.text.toString().trim().toDoubleOrNull()
        val precioVipVal = binding.etPrecioVip.text.toString().trim().toDoubleOrNull()
        val precioMayoristaVal = binding.etPrecioMayorista.text.toString().trim().toDoubleOrNull()
        val categoriaIdSel = categorias.getOrNull(binding.spCategory.selectedItemPosition)?.id
        val marcaIdSel = marcas.getOrNull(binding.spBrand.selectedItemPosition)?.id

        // Validaciones básicas para evitar 500 por datos fuera de rango
        if (mlVal != null && mlVal !in 1..10000) { binding.etMl.error = "Fuera de rango (1-10000)"; return }
        if (stockVal != null && stockVal < 0) { binding.etStock.error = "Stock negativo"; return }
        if (precioDetalleVal != null && precioDetalleVal < 0) { binding.etPrecioDetalle.error = "Precio inválido"; return }
        if (precioVipVal != null && precioVipVal < 0) { binding.etPrecioVip.error = "Precio inválido"; return }
        if (precioMayoristaVal != null && precioMayoristaVal < 0) { binding.etPrecioMayorista.error = "Precio inválido"; return }

        val servicio = ApiClient.getRetrofit(this).create(XanoProducInvService::class.java)
        lifecycleScope.launch {
            try {
                val body = XanoProductoPatchRequest(
                    producto_id = productoId,
                    nombre_producto = nombre,
                    descripcion = descripcion,
                    ml = mlVal,
                    presentacion = presentacionVal,
                    categoria_id = categoriaIdSel,
                    marca_id = marcaIdSel,
                    stock = stockVal,
                    costo_referencia = costoRefVal,
                    precio_detalle = precioDetalleVal,
                    precio_vip = precioVipVal,
                    precio_mayorista = precioMayoristaVal
                )
                val resp = servicio.editarProductoSimple(productoId, body)
                Toast.makeText(this@EditProductActivity, "Actualizado", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@EditProductActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun eliminarProducto() {
        val servicio = ApiClient.getRetrofit(this).create(XanoProducInvService::class.java)
        binding.btnEliminarProducto.isEnabled = false
        lifecycleScope.launch {
            try {
                val resp = servicio.eliminarProducto(productoId, XanoEliminarProductoBody(productoId))
                if (resp.success == true) {
                    Toast.makeText(this@EditProductActivity, resp.message ?: "Eliminado", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@EditProductActivity, resp.message ?: "Error eliminando", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditProductActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnEliminarProducto.isEnabled = true
            }
        }
    }

    private fun cargarDatosProductoYListas() {
        binding.btnSubmit.isEnabled = false
        binding.spCategory.isEnabled = false
        binding.spBrand.isEnabled = false
        val retrofit = ApiClient.getRetrofit(this)
        val catalogoSrv = retrofit.create(XanoCatalogoCompletoService::class.java)
        val producSrv = retrofit.create(XanoProducInvService::class.java) // se mantiene para patch
        val adminSrv = retrofit.create(XanoProducInvAdminService::class.java)
        lifecycleScope.launch {
            try {
                // Intentar usar cache fresco primero para listas
                if (!CatalogCache.isFresh()) {
                    val catalogo = catalogoSrv.obtener()
                    CatalogCache.set(catalogo.categorias, catalogo.marcas)
                }
                categorias = CatalogCache.getCategorias()
                marcas = CatalogCache.getMarcas()
                val nombresCat = categorias.map { it.nombre_categoria ?: "(Sin nombre)" }
                val nombresMarca = marcas.map { it.nombre_marca ?: "(Sin nombre)" }
                binding.spCategory.adapter = ArrayAdapter(this@EditProductActivity, android.R.layout.simple_spinner_dropdown_item, nombresCat)
                binding.spBrand.adapter = ArrayAdapter(this@EditProductActivity, android.R.layout.simple_spinner_dropdown_item, nombresMarca)

                // Preferir detalle admin (incluye más campos e imágenes)
                val det = adminSrv.obtener(productoId)
                if (det == null || det.id == null) {
                    Toast.makeText(this@EditProductActivity, "Detalle admin (id=$productoId) no encontrado", Toast.LENGTH_LONG).show(); finish(); return@launch
                }
                prefillDesdeAdmin(det)
            } catch (e: Exception) {
                Toast.makeText(this@EditProductActivity, "Error cargando datos: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnSubmit.isEnabled = true
                binding.spCategory.isEnabled = true
                binding.spBrand.isEnabled = true
            }
        }
    }
    private fun prefillDesdeAdmin(p: ProducInvAdminDetalle) {
        binding.etSku.setText(p.sku ?: "")
        binding.etName.setText(p.nombre ?: "")
        binding.etDescription.setText(p.descripcion ?: "")
        binding.etMl.setText(p.ml?.toString() ?: "")
        binding.etPresentacion.setText(p.presentacion ?: "")
        binding.etStock.setText(p.stock?.toString() ?: "")
        binding.etCostoReferencia.setText(p.costo_referencia?.toString() ?: "")
        binding.etPrecioDetalle.setText(p.precio_detalle?.toString() ?: "")
        binding.etPrecioVip.setText(p.precio_vip?.toString() ?: "")
        binding.etPrecioMayorista.setText(p.precio_mayorista?.toString() ?: "")
        val idxCat = categorias.indexOfFirst { it.id == p.categoria_id }
        if (idxCat >= 0) binding.spCategory.setSelection(idxCat)
        val idxMarca = marcas.indexOfFirst { it.id == p.marca_id }
        if (idxMarca >= 0) binding.spBrand.setSelection(idxMarca)
        val principal = p.imagenes?.firstOrNull { it.img_principal == true }?.url_imagen?.url
        binding.tvPrincipalInfo.text = principal?.let { "Principal existente: ${it.split('/').last()}" } ?: "Sin imagen principal"
    }

    // Sin manejo de archivos en edición ahora
}
