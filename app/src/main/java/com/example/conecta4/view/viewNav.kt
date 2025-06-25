// package com.example.conecta4.view

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.conecta4.util.navRutas
import com.example.conecta4.view.GameScreen
import com.example.conecta4.view.MultiplayerGameScreen
import com.example.conecta4.view.ViewCreateRoom
import com.example.conecta4.view.ViewHome
import com.example.conecta4.view.ViewInicioSesion
import com.example.conecta4.view.ViewJoinRoom
import com.example.conecta4.view.ViewRegistro
import com.example.conecta4.view.WaitingRoomScreen
import com.example.conecta4.viewModel.GameViewModel
import com.example.conecta4.viewModel.RoomViewModel // Importar RoomViewModel
import com.example.conecta4.viewModel.UsuarioViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val authViewModel: UsuarioViewModel = viewModel()
    val roomViewModel: RoomViewModel = viewModel()
    val gameViewModel: GameViewModel = viewModel() // <--- Instanciar GameViewModel aquí

    NavHost(
        navController = navController,
        startDestination = navRutas.iniSes
    ) {
        composable(navRutas.iniSes) {
            ViewInicioSesion(
                usuarioViewModel = authViewModel,
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
            ViewHome(
                navController = navController,
                usuarioViewModel = authViewModel
            )
        }

        composable(navRutas.jueMaq) {
            GameScreen(navController = navController) // Tu juego contra la máquina
        }

        composable(navRutas.createRoom) {
            ViewCreateRoom(
                navController = navController,
                roomViewModel = roomViewModel
            )
        }
        composable(navRutas.joinRoom) {
            ViewJoinRoom(
                navController = navController,
                roomViewModel = roomViewModel
            )
        }
        composable("${navRutas.waitingRoom}/{roomId}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId")
            if (roomId != null) {
                WaitingRoomScreen(
                    navController = navController,
                    roomId = roomId,
                    roomViewModel = roomViewModel,
                    gameViewModel = gameViewModel // Pasa el gameViewModel para inicializar el juego
                )
            } else {
                navController.popBackStack()
            }
        }
        // --- RUTA PARA EL JUEGO MULTIJUGADOR ---
        composable("${navRutas.gameRoom}/{roomId}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId")
            if (roomId != null) {
                MultiplayerGameScreen(
                    navController = navController,
                    roomId = roomId,
                    gameViewModel = gameViewModel // Pasa el gameViewModel
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}