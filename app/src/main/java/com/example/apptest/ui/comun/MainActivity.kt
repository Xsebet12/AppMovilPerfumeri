package com.example.apptest.ui.comun

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavOptions
import androidx.navigation.ui.NavigationUI
import com.example.apptest.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.apptest.core.storage.SessionManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configuración de la Barra de Navegación 
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Conecto la barra de navegación con el NavController para la navegación automática.
        // Determinar tipo de usuario para elegir destino inicial dinámico.
        val sesion = SessionManager.getInstance(applicationContext).getUser()
        val esEmpleado = sesion?.user_type?.equals("empleado", true) == true
        val esOwner = sesion?.rol_id == 2

        // Inflamos el navGraph y ajustamos dinámicamente el startDestination.
        val inflater = navController.navInflater
        val graph = inflater.inflate(R.navigation.nav_graph)
        // Usar setter compatible con versiones antiguas de Navigation
        graph.setStartDestination(if (esEmpleado) R.id.homeAdminFragment else R.id.homeFragment)
        navController.graph = graph

        // Conectar bottom navigation DESPUÉS de asignar el graph definitivo.
        NavigationUI.setupWithNavController(bottomNavigationView, navController)

        // Personalizar navegación para limpiar detalle intermedio (product detail fragment) al cambiar secciones
        bottomNavigationView.setOnItemSelectedListener { item ->
            val destino = item.itemId
            // Construir navOptions para hacer pop hasta el home correspondiente, sin incluirlo
            val popBase = if (esEmpleado) R.id.homeAdminFragment else R.id.homeFragment
            val opts = NavOptions.Builder()
                .setPopUpTo(popBase, false)
                .setLaunchSingleTop(true)
                .build()
            // Evitar navegación redundante si ya estamos en destino
            if (navController.currentDestination?.id == destino) return@setOnItemSelectedListener true
            navController.navigate(destino, null, opts)
            true
        }

        // Ajustar ítems visibles según tipo de usuario
        if (esEmpleado) {
            bottomNavigationView.menu.findItem(R.id.homeFragment)?.isVisible = false
            bottomNavigationView.menu.findItem(R.id.homeAdminFragment)?.isVisible = true
            bottomNavigationView.menu.findItem(R.id.cartFragment)?.isVisible = false
            bottomNavigationView.menu.findItem(R.id.manageOrdersFragment)?.isVisible = true
            // Seleccionar explícitamente el item HomeAdmin para marcarlo
            bottomNavigationView.selectedItemId = R.id.homeAdminFragment
        } else {
            bottomNavigationView.menu.findItem(R.id.homeAdminFragment)?.isVisible = false
            bottomNavigationView.menu.findItem(R.id.homeFragment)?.isVisible = true
            bottomNavigationView.selectedItemId = R.id.homeFragment
        }

        // Visibilidad de gestión de usuarios (todos los empleados pueden ver clientes)
        bottomNavigationView.menu.findItem(R.id.manageUsersFragment)?.isVisible = esEmpleado
        bottomNavigationView.menu.findItem(R.id.manageOrdersFragment)?.isVisible = esEmpleado
    }

    override fun onResume() {
        super.onResume()
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)

        val sesion = SessionManager.getInstance(applicationContext).getUser()
        val esEmpleado = sesion?.user_type?.equals("empleado", true) == true
        val esOwner = sesion?.rol_id == 2

        // Reaplicar visibilidad y destino inicial si cambió el perfil
        val inflater = navController.navInflater
        val graph = inflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(if (esEmpleado) R.id.homeAdminFragment else R.id.homeFragment)
        navController.graph = graph

        bottomNavigationView.menu.findItem(R.id.homeAdminFragment)?.isVisible = esEmpleado
        bottomNavigationView.menu.findItem(R.id.homeFragment)?.isVisible = !esEmpleado
        bottomNavigationView.menu.findItem(R.id.cartFragment)?.isVisible = !esEmpleado
        bottomNavigationView.menu.findItem(R.id.manageUsersFragment)?.isVisible = esEmpleado
        bottomNavigationView.menu.findItem(R.id.manageOrdersFragment)?.isVisible = esEmpleado
        bottomNavigationView.selectedItemId = if (esEmpleado) R.id.homeAdminFragment else R.id.homeFragment
    }
}
