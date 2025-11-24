package com.example.apptest.ui.empleado

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.apptest.R
import com.example.apptest.user.services.XanoProducInvItem
import com.example.apptest.databinding.ItemAdminProductBinding

class AdminProductAdapter(
    private val onClick: (XanoProducInvItem) -> Unit
) : ListAdapter<XanoProducInvItem, AdminProductAdapter.VH>(Diff()) {

    inner class VH(val b: ItemAdminProductBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        val binding = ItemAdminProductBinding.inflate(inf, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.b.tvName.text = item.nombre_producto
        val marcaTxt = item.nombre_marca?.takeIf { it.isNotBlank() }
        val catTxt = item.nombre_categoria?.takeIf { it.isNotBlank() }
        holder.b.tvMarcaCategoria.text = listOfNotNull(marcaTxt, catTxt).joinToString(" • ")
        holder.b.tvStock.text = "Stock: ${item.stock ?: '-'}"
        holder.b.tvPrecios.text = buildString {
            append("Det: ")
            append(item.precio_detalle?.let { formatear(it) } ?: "-")
            append("  Vip: ")
            append(item.precio_vip?.let { formatear(it) } ?: "-")
            append("  May: ")
            append(item.precio_mayorista?.let { formatear(it) } ?: "-")
        }
        holder.b.tvEstado.text = buildString {
            append(if (item.disponible == true) "Disponible" else "No disponible")
            append(" / ")
            append(if (item.habilitado == true) "Habilitado" else "Deshabilitado")
        }
        val colorEstado = if (item.disponible == true && item.habilitado == true) R.color.estado_ok else R.color.estado_no
        holder.b.tvEstado.setTextColor(holder.b.tvEstado.context.getColor(colorEstado))
        val firstImage = if (!item.url_imagen_principal.isNullOrBlank()) item.url_imagen_principal else item.url_imagen_extras.firstOrNull()
        holder.b.ivThumb.load(firstImage) {
            crossfade(true)
            placeholder(R.drawable.ic_product_placeholder)
            error(R.drawable.ic_product_placeholder)
            fallback(R.drawable.ic_product_placeholder)
        }
        holder.b.root.setOnClickListener { onClick(item) }
    }

    private fun formatear(v: Double): String = try {
        val nf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("es-CL"))
        nf.maximumFractionDigits = 0
        nf.format(v)
    } catch (_: Exception) { "$${"%.0f".format(v)}" }

    class Diff : DiffUtil.ItemCallback<XanoProducInvItem>() {
        override fun areItemsTheSame(oldItem: XanoProducInvItem, newItem: XanoProducInvItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: XanoProducInvItem, newItem: XanoProducInvItem): Boolean = oldItem == newItem
    }
}
