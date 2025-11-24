package com.example.apptest.ui.empleado

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.util.Log
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.apptest.R
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserDetailFragment: Fragment() {
    private val clienteId: Int by lazy { arguments?.getInt("cliente_id") ?: -1 }
    private val viewModel: UserDetailViewModel by viewModels { object: ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return UserDetailViewModel(requireContext().applicationContext, clienteId) as T
        }
    } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_user_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvNombre = view.findViewById<TextView>(R.id.tvNombreDetalle)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmailDetalle)
        val tvEstado = view.findViewById<TextView>(R.id.tvEstadoDetalle)
        val btnEditar = view.findViewById<View>(R.id.btnEditarDetalle)
        val btnToggle = view.findViewById<View>(R.id.btnToggleDetalle)
        val btnEliminar = view.findViewById<View>(R.id.btnEliminarDetalle)
        val progress = view.findViewById<View>(R.id.progressDetalle)

        lifecycleScope.launch {
            viewModel.state.collectLatest { st ->
                progress.visibility = if (st.cargando || st.guardando) View.VISIBLE else View.GONE
                val c = st.cliente
                if (c != null) {
                    val nombre = listOfNotNull(c.primer_nombre, c.segundo_nombre, c.apellido_paterno, c.apellido_materno).joinToString(" ").trim()
                    tvNombre.text = if (nombre.isNotBlank()) nombre else "(Sin nombre)"
                    tvEmail.text = c.email_contacto ?: "-"
                    tvEstado.text = if (c.habilitado == true) "Habilitado" else "Bloqueado"
                }
            }
        }

        btnEditar.setOnClickListener { mostrarDialogoEdicion() }
        btnToggle.setOnClickListener { viewModel.toggleHabilitado() }
        btnEliminar.setOnClickListener { confirmarEliminar() }
    }

    private fun mostrarDialogoEdicion() {
        val st = viewModel.state.value
        val c = st.cliente ?: return
        val v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_editar_cliente, null)
        val etPrimer = v.findViewById<TextInputEditText>(R.id.etPrimerNombre)
        val etSegundo = v.findViewById<TextInputEditText>(R.id.etSegundoNombre)
        val etApPat = v.findViewById<TextInputEditText>(R.id.etApellidoPaterno)
        val etApMat = v.findViewById<TextInputEditText>(R.id.etApellidoMaterno)
        val etEmail = v.findViewById<TextInputEditText>(R.id.etEmail)
        val etTelefono = v.findViewById<TextInputEditText>(R.id.etTelefono)
        val etCalle = v.findViewById<TextInputEditText>(R.id.etCalle)
        val etNumero = v.findViewById<TextInputEditText>(R.id.etNumero)
        val etPassword = v.findViewById<TextInputEditText>(R.id.etPassword)
        val spRegion = v.findViewById<Spinner>(R.id.spRegion)
        val spComuna = v.findViewById<Spinner>(R.id.spComuna)
        etPassword.visibility = View.GONE // no cambio contraseña desde aquí

        etPrimer.setText(c.primer_nombre)
        etSegundo.setText(c.segundo_nombre)
        etApPat.setText(c.apellido_paterno)
        etApMat.setText(c.apellido_materno)
        etEmail.setText(c.email_contacto)
        etTelefono.setText(c.telefono_contacto)
        etCalle.setText(c.nombre_calle)
        etNumero.setText(c.numero_calle)

        // Poblar spinners
        val regiones = st.regiones.map { it.second }
        spRegion.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, regiones)
        val comunas = st.comunas.map { it.second }
        spComuna.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, comunas)

        // Selecciones iniciales si existen
        c.comuna_id?.let { cid ->
            val idxComuna = st.comunas.indexOfFirst { it.first == cid }
            if (idxComuna >= 0) spComuna.setSelection(idxComuna)
            val rid = st.comunasDetalladas.firstOrNull { it.first == cid }?.third
            if (rid != null) {
                val idxRegion = st.regiones.indexOfFirst { it.first == rid }
                if (idxRegion >= 0) spRegion.setSelection(idxRegion)
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Editar Cliente")
            .setView(v)
            .setPositiveButton("Guardar") { _, _ ->
                val datos = mutableMapOf<String, Any?>()
                datos["primer_nombre"] = etPrimer.text?.toString()?.takeIf { it.isNotBlank() }
                datos["segundo_nombre"] = etSegundo.text?.toString()?.takeIf { it.isNotBlank() }
                datos["apellido_paterno"] = etApPat.text?.toString()?.takeIf { it.isNotBlank() }
                datos["apellido_materno"] = etApMat.text?.toString()?.takeIf { it.isNotBlank() }
                datos["email_contacto"] = etEmail.text?.toString()?.takeIf { it.isNotBlank() }
                datos["telefono_contacto"] = etTelefono.text?.toString()?.takeIf { it.isNotBlank() }
                datos["nombre_calle"] = etCalle.text?.toString()?.takeIf { it.isNotBlank() }
                datos["numero_calle"] = etNumero.text?.toString()?.takeIf { it.isNotBlank() }
                val ridx = spRegion.selectedItemPosition
                val cidx = spComuna.selectedItemPosition
                val st2 = viewModel.state.value
                if (ridx >= 0 && ridx < st2.regiones.size) datos["region_id"] = st2.regiones[ridx].first
                if (cidx >= 0 && cidx < st2.comunas.size) datos["comuna_id"] = st2.comunas[cidx].first
                viewModel.guardarCambios(datos) { ok -> if (!ok) Toast.makeText(requireContext(), "Error guardando", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            val color = androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.black)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(color)
        }
        dialog.show()
    }

    private fun confirmarEliminar() {
        val dialog = AlertDialog.Builder(requireContext())
            .setMessage("¿Eliminar definitivamente este cliente?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminar { ok, msg ->
                    if (ok) {
                        parentFragmentManager.setFragmentResult("cliente_eliminado", Bundle())
                        Toast.makeText(requireContext(), msg ?: "Cliente eliminado", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    } else {
                        Log.e("UserDetailFragment", "Fallo eliminar: $msg")
                        Toast.makeText(requireContext(), "Error eliminando: ${msg ?: "desconocido"}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            val color = androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.black)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(color)
        }
        dialog.show()
    }
}
