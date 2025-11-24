package com.example.apptest.ui.cliente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apptest.cliente.services.HistorialDetalle
import com.example.apptest.cliente.services.HistorialPedido
import com.example.apptest.core.network.ApiClient
import com.example.apptest.cliente.services.XanoVentaService
import com.example.apptest.databinding.ActivityPurchaseHistoryBinding
import com.example.apptest.databinding.ItemHistoryDetailBinding
import com.example.apptest.databinding.ItemHistoryHeaderBinding
import com.example.apptest.core.storage.SessionManager
import android.util.Log
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class PurchaseHistoryActivity : ComponentActivity() {
    private lateinit var binding: ActivityPurchaseHistoryBinding
    private val items: MutableList<HistoryListItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPurchaseHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajustar padding superior dinámico para status bar + separación
        val extraTopDp = 12
        val extraTopPx = (extraTopDp * resources.displayMetrics.density).toInt()
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val sb = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, sb.top + extraTopPx, v.paddingRight, v.paddingBottom)
            insets
        }

        binding.recyclerHistorial.layoutManager = LinearLayoutManager(this)
        val adapter = HistoryAdapter(items)
        binding.recyclerHistorial.adapter = adapter

        lifecycleScope.launch {
            try {
                val servicio = ApiClient.getRetrofit(this@PurchaseHistoryActivity).create(XanoVentaService::class.java)
                val userId = SessionManager.getInstance(this@PurchaseHistoryActivity).getUser()?.id
                if (userId == null) {
                    // No hay usuario en sesión; cerrar la pantalla silenciosamente
                    Log.w("Historial", "Usuario no encontrado en sesión; se cierra la actividad")
                    finish()
                    return@launch
                }
                Log.d("Historial", "Solicitando historial para userId=$userId")
                val lista = servicio.historialComprasCliente(userId)
                Log.d("Historial", "Respuesta historial size=${lista.size}")
                val planos = flatten(lista)
                Log.d("Historial", "Items planos size=${planos.size}")
                items.clear(); items.addAll(planos)
                adapter.update(items)
                // Fallback por si DiffUtil no refresca (no debería pasar)
                binding.recyclerHistorial.post {
                    adapter.notifyDataSetChanged()
                    Log.d("Historial", "notifyDataSetChanged ejecutado. Total items=${adapter.itemCount}")
                }
            } catch (_: Exception) {
                Log.e("Historial", "Error obteniendo historial" )
                finish()
            }
        }
    }

    private fun flatten(src: List<HistorialPedido>): List<HistoryListItem> {
        val out = mutableListOf<HistoryListItem>()
        src.forEach { pedido ->
            out.add(HistoryListItem.Header(pedido.pedido_info))
            pedido.detalles.forEach { d ->
                out.add(HistoryListItem.Detail(d))
            }
        }
        return out
    }
}

private sealed class HistoryListItem {
    data class Header(val texto: String): HistoryListItem()
    data class Detail(val d: HistorialDetalle): HistoryListItem()
}

private class HistoryAdapter(private var items: List<HistoryListItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private companion object { const val T_HEADER = 1; const val T_DETAIL = 2 }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is HistoryListItem.Header -> T_HEADER
        is HistoryListItem.Detail -> T_DETAIL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == T_HEADER) {
            val b = ItemHistoryHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            object: RecyclerView.ViewHolder(b.root) {}
        } else {
            val b = ItemHistoryDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            object: RecyclerView.ViewHolder(b.root) {}
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val it = items[position]) {
            is HistoryListItem.Header -> {
                val b = ItemHistoryHeaderBinding.bind(holder.itemView)
                b.tvHeader.text = it.texto
            }
            is HistoryListItem.Detail -> {
                val b = ItemHistoryDetailBinding.bind(holder.itemView)
                b.tvNombreProducto.text = it.d.nombre_producto ?: "Producto"
                b.tvCantidadPrecio.text = "${it.d.cantidad} x ${formatearCLP(it.d.precio_unitario)}"
                b.tvSubtotal.text = formatearCLP(it.d.subtotal)
            }
        }
    }

    fun update(nuevos: List<HistoryListItem>) {
        if (items.isEmpty()) {
            items = nuevos
            notifyItemRangeInserted(0, nuevos.size)
            Log.d("HistorialAdapter", "Insertados iniciales=${nuevos.size}")
            return
        }
        val diff = DiffUtil.calculateDiff(object: DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            override fun getNewListSize(): Int = nuevos.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = items[oldItemPosition] == nuevos[newItemPosition]
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = items[oldItemPosition] == nuevos[newItemPosition]
        })
        items = nuevos
        diff.dispatchUpdatesTo(this)
        Log.d("HistorialAdapter", "Diff aplicado. Nuevo total=${items.size}")
    }

    private fun formatearCLP(valor: Double): String {
        return try {
            val locale = java.util.Locale.forLanguageTag("es-CL")
            val nf = java.text.NumberFormat.getCurrencyInstance(locale)
            nf.maximumFractionDigits = 0
            nf.format(valor)
        } catch (_: Exception) { "$${"%.0f".format(valor)}" }
    }
}
