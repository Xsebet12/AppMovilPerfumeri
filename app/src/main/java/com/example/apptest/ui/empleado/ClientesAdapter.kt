package com.example.apptest.ui.empleado

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apptest.R
import com.example.apptest.cliente.models.XanoCliente

class ClientesAdapter(
    private var items: List<XanoCliente>,
    private val onDetalle: (XanoCliente) -> Unit
): RecyclerView.Adapter<ClientesAdapter.VH>() {

    fun submit(list: List<XanoCliente>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cliente, parent, false)
        return VH(v)
    }
    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(view: View): RecyclerView.ViewHolder(view) {
        private val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        private val tvEmail: TextView = view.findViewById(R.id.tvEmail)
        private val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        // Botones removidos del layout; interacción es solo por click del item
        fun bind(c: XanoCliente) {
            val nombre = listOfNotNull(c.primer_nombre, c.segundo_nombre, c.apellido_paterno, c.apellido_materno).joinToString(" ").trim()
            tvNombre.text = if (nombre.isNotBlank()) nombre else "(Sin nombre)"
            tvEmail.text = c.email_contacto ?: "-"
            tvEstado.text = if (c.habilitado == true) "Habilitado" else "Bloqueado"
            itemView.setOnClickListener { onDetalle(c) }
        }
    }
}
