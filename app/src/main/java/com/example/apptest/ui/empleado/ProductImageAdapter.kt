package com.example.apptest.ui.empleado

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.apptest.user.services.ProducInvAdminImagen
import com.example.apptest.databinding.ItemProductoImagenBinding

class ProductImageAdapter(
    private val onMarkPrincipal: (ProducInvAdminImagen) -> Unit,
    private val onDelete: (ProducInvAdminImagen) -> Unit
) : ListAdapter<ProducInvAdminImagen, ProductImageAdapter.VH>(DIFF) {

    object DIFF : DiffUtil.ItemCallback<ProducInvAdminImagen>() {
        override fun areItemsTheSame(oldItem: ProducInvAdminImagen, newItem: ProducInvAdminImagen): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ProducInvAdminImagen, newItem: ProducInvAdminImagen): Boolean = oldItem == newItem
    }

    inner class VH(val b: ItemProductoImagenBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(img: ProducInvAdminImagen) {
            val url = img.url_imagen?.url
            if (url != null) b.imgPreview.load(url)
            b.tvNombre.text = url?.split('/')?.last() ?: "(sin url)"
            b.tvPrincipal.text = if (img.img_principal == true) "Principal" else "Extra"
            b.btnPrincipal.isEnabled = img.img_principal != true
            b.btnPrincipal.setOnClickListener { onMarkPrincipal(img) }
            b.btnEliminar.setOnClickListener { onDelete(img) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemProductoImagenBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
