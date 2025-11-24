package com.example.apptest.ui.comun

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import coil.load
import com.example.apptest.R
import com.example.apptest.user.services.XanoProducInvItem
import com.example.apptest.core.storage.SessionManager
import com.example.apptest.databinding.ItemProductBinding
import com.example.apptest.BuildConfig
import android.graphics.drawable.ColorDrawable
import android.graphics.Color

class ProductAdapter(
    private var items: List<XanoProducInvItem>,
    private val onClick: (XanoProducInvItem) -> Unit
) : RecyclerView.Adapter<ProductAdapter.VH>() {

    init { setHasStableIds(true) }

    inner class VH(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items[position].id

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.nombre_producto
        // Precio dinámico según tipo de cliente desde sesión
        val tipoCliente = SessionManager.getInstance(holder.binding.root.context).getUser()?.tipo_cliente?.lowercase()
        val precio = when (tipoCliente) {
            "vip" -> item.precio_vip ?: item.precio_detalle
            "mayorista" -> item.precio_mayorista ?: item.precio_detalle
            else -> item.precio_detalle
        }
        holder.binding.tvPrice.text = precio?.let { formatearCLP(it) } ?: ""
        holder.binding.tvStockList.text = item.stock?.let { "Stock: $it" } ?: ""
        // Marca y Categoría por nombre 
        val lineaSec = buildString {
            val marcaTxt = item.nombre_marca?.takeIf { it.isNotBlank() }
            val catTxt = item.nombre_categoria?.takeIf { it.isNotBlank() }
            if (!marcaTxt.isNullOrBlank()) append(marcaTxt)
            if (!catTxt.isNullOrBlank()) {
                if (isNotEmpty()) append(" • ")
                append(catTxt)
            }
        }
        holder.binding.tvMarcaCategoria.text = lineaSec
        // Imagen principal o primera extra
        val firstImage = if (!item.url_imagen_principal.isNullOrBlank()) item.url_imagen_principal
            else item.url_imagen_extras.firstOrNull { it.isNotBlank() }
        val url = firstImage?.let { normalizarUrl(it) }
        holder.binding.ivThumb.load(url) {
            crossfade(true)
            placeholder(R.drawable.ic_product_placeholder)
            error(R.drawable.ic_product_placeholder)
            fallback(R.drawable.ic_product_placeholder) 
        }
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    fun updateItems(newItems: List<XanoProducInvItem>) {
        val diff = DiffUtil.calculateDiff(object: DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            override fun getNewListSize(): Int = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].id == newItems[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val o = items[oldItemPosition]
                val n = newItems[newItemPosition]
                return o == n
            }
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }

    fun updateItem(updated: XanoProducInvItem) {
        val index = items.indexOfFirst { it.id == updated.id }
        if (index == -1) return
        val mutable = items.toMutableList()
        mutable[index] = updated
        updateItems(mutable)
    }

    private fun normalizarUrl(ruta: String): String {
        val r = ruta.trim()
        if (r.startsWith("http", ignoreCase = true)) {
            return try {
                val uri = java.net.URI(r)
                val host = uri.host ?: ""
                if (host == "localhost" || host == "127.0.0.1") {
                    val base = BuildConfig.XANO_BASE_URL.trimEnd('/')
                    val baseUri = java.net.URI(base)
                    val scheme = baseUri.scheme ?: uri.scheme
                    val nuevoHost = baseUri.host ?: host
                    val nuevoPuerto = if (baseUri.port == -1) uri.port else baseUri.port
                    val nuevaUri = java.net.URI(scheme, null, nuevoHost, nuevoPuerto, uri.path, uri.query, uri.fragment)
                    nuevaUri.toString()
                } else {
                    r
                }
            } catch (_: Exception) {
                r
            }
        }
        // Si es ruta relativa, la unimos a la base de Xano
        val rel = r.trimStart('/')
        return BuildConfig.XANO_BASE_URL.trimEnd('/') + "/" + rel
    }

    private fun formatearCLP(valor: Double): String {
        return try {
            val locale = java.util.Locale.forLanguageTag("es-CL")
            val nf = java.text.NumberFormat.getCurrencyInstance(locale)
            nf.maximumFractionDigits = 0
            nf.format(valor)
        } catch (_: Exception) {
            "$${"%.0f".format(valor)}"
        }
    }
}