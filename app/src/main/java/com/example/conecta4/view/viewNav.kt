package com.example.conecta4.view

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.conecta4.util.navRutas
import com.example.conecta4.viewModel.UsersListViewModel
import com.example.conecta4.viewModel.UsuarioViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Crear solo una instancia del ViewModel de autenticación
    val authViewModel: UsuarioViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = navRutas.iniSes
    ) {
        composable(navRutas.iniSes) {
            ViewInicioSesion(
                usuarioViewModel = authViewModel, // ✅ PASAR el ViewModel aquí
                onNavigateToRegister = {
                    navController.navigate(navRutas.registro)
                },
                onLoginSuccess = {
                    navController.navigate(navRutas.inicio)
                }
            )
        }

        composable(navRutas.registro) {
            ViewRegistro(
                navController = navController,
                usuarioViewModel = authViewModel
            )
        }

        composable(navRutas.inicio) {
            ViewHome(navController = navController)
        }

        composable(navRutas.jueMaq) {
            GameScreen(navController = navController)
        }

        composable(navRutas.usersList) {
            val viewModel = viewModel<UsersListViewModel>()
            UsersListScreen(navController = navController, usersListViewModel = viewModel)
        }
    }
}
