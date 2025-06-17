package com.example.conecta4.view

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.conecta4.PantallaPrincipal
import com.example.conecta4.util.navRutas
import com.example.conecta4.viewModel.UsuarioViewModel


@Composable
fun AppNavigation() {

    // rememberNavController() crea y recuerda un NavController para este NavHost
    val navController = rememberNavController()

   val authViewModel: UsuarioViewModel = viewModel()

    // El NavHost es donde defines tus destinos de navegación
    NavHost(
        navController = navController,
        startDestination = navRutas.iniSes // Establece la pantalla de registro como el inicio
    ) {

        // Define el destino para la pantalla de registro
        composable(navRutas.iniSes) {
            ViewInicioSesion(

                // authViewModel = authViewModel, // Pásalo si lo necesitas en ViewInicioSesion
                onNavigateToRegister = {
                    // Navegar a la pantalla de registro
                    navController.navigate(navRutas.registro)
                },
                onLoginSuccess = {
                    // Navegar a la pantalla principal (HOME) después de un inicio de sesión exitoso
                    // Usar popUpTo para limpiar la pila de retroceso y que no puedas volver a la pantalla de login
                    navController.navigate(navRutas.inicio)
                }

            )
        }
        composable(navRutas.registro){
            // Aquí es donde llamas a tu ViewRegistro Composable
            ViewRegistro(
                navController,
                authViewModel // Pasas la instancia del ViewModel

            )
        }
        composable(navRutas.inicio){
            ViewHome(navController = navController)
        }
        composable(navRutas.jueMaq){
            PantallaPrincipal()
        }


    }
}