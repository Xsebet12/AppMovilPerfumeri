package com.example.apptest.ui.empleado

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.apptest.R

class AdminImagesPagerAdapter(
    private var images: List<String>
) : RecyclerView.Adapter<AdminImagesPagerAdapter.ImageVH>() {

    inner class ImageVH(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageVH {
        val iv = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_image, parent, false) as ImageView
        return ImageVH(iv)
    }

    override fun getItemCount(): Int = images.size

    override fun onBindViewHolder(holder: ImageVH, position: Int) {
        val url = images[position]
        holder.imageView.load(url) {
            crossfade(true)
            placeholder(R.drawable.ic_product_placeholder)
            error(R.drawable.ic_product_placeholder)
        }
    }

    fun submit(list: List<String>) {
        images = list
        notifyDataSetChanged()
    }
}