package com.example.apptest.ui.empleado

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import androidx.viewpager2.widget.ViewPager2
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.apptest.R
import com.example.apptest.user.services.XanoProducInvItem
import com.example.apptest.user.services.XanoProducInvAdminService
import com.example.apptest.user.services.XanoProducInvService
import com.example.apptest.user.services.ProducInvAdminDetalle
import com.example.apptest.core.network.ApiClient
import com.example.apptest.databinding.FragmentAdminProductDetailBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AdminProductDetailFragment : Fragment() {
    private var _binding: FragmentAdminProductDetailBinding? = null
    private val binding get() = _binding!!

    private var productId: Long = -1L
    private var imagesAdapter: AdminImagesPagerAdapter? = null
    private var pageCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        productId = arguments?.getLong("product_id", -1L) ?: -1L
        if (productId == -1L) {
            binding.tvNombre.text = "Producto no válido"
            return
        }

        cargarDetalle()
    }

    private fun cargarDetalle() {
        binding.progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.getRetrofit(requireContext()).create(XanoProducInvAdminService::class.java)
                val detalle = api.obtener(productId)
                if (detalle == null || detalle.id == null) {
                    binding.tvNombre.text = "Producto no encontrado"
                } else {
                    bind(mapDetalle(detalle))
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    binding.tvNombre.text = e.message ?: "Error cargando producto"
                }
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun mapDetalle(src: ProducInvAdminDetalle): XanoProducInvItem {
        val principal = src.imagenes?.firstOrNull { it.img_principal == true }?.url_imagen?.url
        val extras = src.imagenes?.filter { it.img_principal != true }?.mapNotNull { it.url_imagen?.url } ?: emptyList()
        return XanoProducInvItem(
            id = src.id!!,
            inventario_id = null, // no viene en detalle individual
            created_at = null,
            sku = src.sku,
            nombre_producto = src.nombre ?: "",
            descripcion = src.descripcion,
            ml = src.ml,
            presentacion = src.presentacion,
            categoria_id = src.categoria_id,
            marca_id = src.marca_id,
            nombre_categoria = src.categoria,
            nombre_marca = src.marca,
            stock = src.stock,
            disponible = src.disponible,
            habilitado = src.habilitado,
            precio_detalle = src.precio_detalle,
            precio_vip = src.precio_vip,
            precio_mayorista = src.precio_mayorista,
            costo_referencia = src.costo_referencia,
            url_imagen_principal = principal,
            url_imagen_extras = extras
        )
    }

    private fun bind(item: XanoProducInvItem) {
        // Campos básicos
        binding.tvNombre.text = item.nombre_producto
        binding.tvSku.text = item.sku?.let { "SKU: $it" } ?: "SKU: N/D"
        binding.tvMarca.text = item.nombre_marca?.let { "Marca: $it" } ?: "Marca: N/D"
        binding.tvCategoria.text = item.nombre_categoria?.let { "Categoría: $it" } ?: "Categoría: N/D"
        binding.tvStock.text = item.stock?.let { "Stock: $it" } ?: "Stock: N/D"
        binding.tvPrecioDetalle.text = "Precio detalle: ${formatearCLP(item.precio_detalle)}"
        binding.tvPrecioVip.text = "Precio VIP: ${formatearCLP(item.precio_vip)}"
        binding.tvPrecioMayorista.text = "Precio mayorista: ${formatearCLP(item.precio_mayorista)}"
        binding.tvCosto.text = "Costo referencia: ${formatearCLP(item.costo_referencia)}"

        // Campos adicionales agregados en layout
        binding.tvDescripcion.text = item.descripcion?.takeIf { it.isNotBlank() } ?: "(Sin descripción)"
        val mlPresent = listOfNotNull(
            item.ml?.let { if (it > 0) "${it.toInt()}ml" else null },
            item.presentacion?.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
        binding.tvMlPresentacion.text = if (mlPresent.isNotBlank()) mlPresent else ""
        binding.tvDisponibleHabilitado.text = "Disponible: ${item.disponible?.toString() ?: "N/D"} | Habilitado: ${item.habilitado?.toString() ?: "N/D"}"
        binding.tvCategoriaId.text = "Categoria ID: ${item.categoria_id ?: "--"}"
        binding.tvMarcaId.text = "Marca ID: ${item.marca_id ?: "--"}"

        // Preparar lista de imágenes (principal primero)
        val orderedImages = buildList<String> {
            item.url_imagen_principal?.let { add(it) }
            item.url_imagen_extras.forEach { add(it) }
        }

        if (imagesAdapter == null) {
            imagesAdapter = AdminImagesPagerAdapter(orderedImages)
            binding.pagerImagenes.adapter = imagesAdapter
        } else {
            imagesAdapter?.submit(orderedImages)
        }

        // Indicador inicial
        val total = orderedImages.size
        binding.tvPagerIndicador.text = if (total == 0) "0/0" else "1/$total"

        // Registrar callback para actualizar indicador
        pageCallback?.let { binding.pagerImagenes.unregisterOnPageChangeCallback(it) }
        pageCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tvPagerIndicador.text = if (total == 0) "0/0" else "${position + 1}/$total"
            }
        }
        binding.pagerImagenes.registerOnPageChangeCallback(pageCallback!!)

        // Mostrar botón modificar solo para admin/empleado
        val usuario = com.example.apptest.core.storage.SessionManager.getInstance(requireContext()).getUser()
        val esPriv = when {
            usuario?.rol?.equals("admin", true) == true -> true
            usuario?.rol?.equals("empleado", true) == true -> true
            usuario?.user_type?.equals("empleado", true) == true -> true
            usuario?.user_type?.equals("admin", true) == true -> true
            else -> false
        }
        if (esPriv) {
            binding.btnModificarAdmin.visibility = View.VISIBLE
            binding.btnModificarAdmin.setOnClickListener {
                val i = android.content.Intent(requireContext(), EditProductActivity::class.java)
                i.putExtra("product_id", item.id)
                startActivity(i)
            }
            binding.btnModificarImagenes.visibility = View.VISIBLE
            binding.btnModificarImagenes.setOnClickListener {
                val i = android.content.Intent(requireContext(), ManageProductImagesActivity::class.java)
                i.putExtra("product_id", item.id)
                startActivity(i)
            }
            binding.btnHabilitarDeshabilitar.visibility = View.VISIBLE
            binding.btnHabilitarDeshabilitar.text = if (item.habilitado == true) "Deshabilitar Producto" else "Habilitar Producto"
            binding.btnHabilitarDeshabilitar.setOnClickListener { toggleHabilitado(item) }
        } else {
            binding.btnModificarAdmin.visibility = View.GONE
            binding.btnModificarImagenes.visibility = View.GONE
            binding.btnHabilitarDeshabilitar.visibility = View.GONE
        }
    }

    private fun formatearCLP(valor: Double?): String {
        if (valor == null) return "--"
        return try {
            val nf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("es-CL"))
            nf.maximumFractionDigits = 0
            nf.format(valor)
        } catch (_: Exception) { "${'$'}${"%.0f".format(valor)}" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pageCallback?.let {
            try { binding.pagerImagenes.unregisterOnPageChangeCallback(it) } catch (_: Exception) {}
        }
        _binding = null
    }

    private fun toggleHabilitado(item: XanoProducInvItem) {
        val nuevo = item.habilitado != true
        binding.progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val servicio = ApiClient.getRetrofit(requireContext()).create(XanoProducInvService::class.java)
                val rb = { s: String -> s.toRequestBody("text/plain".toMediaTypeOrNull()) }
                val rbOpt = { v: Any? -> v?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()) }
                // El endpoint exige 'stock' aunque no cambie; enviamos valor actual para evitar 400
                servicio.actualizarEstadoProducto(
                    productoId = rb(item.id.toString()),
                    habilitado = rb(nuevo.toString()),
                    disponible = rbOpt(item.disponible),
                    stock = rbOpt(item.stock)
                )
                Toast.makeText(requireContext(), if (nuevo) "Producto habilitado" else "Producto deshabilitado", Toast.LENGTH_SHORT).show()
                // Refrescar
                cargarDetalle()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error actualizando: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }
}
