package com.example.conecta4.view

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.conecta4.util.navRutas
import com.example.conecta4.viewModel.RoomViewModel
import com.example.conecta4.viewModel.GameViewModel // Importar GameViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch // Importar launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitingRoomScreen(
    navController: NavController,
    roomId: String, // Recibe el ID de la sala
    roomViewModel: RoomViewModel = viewModel(),
    gameViewModel: GameViewModel = viewModel() // Recibe el GameViewModel
) {
    val context = LocalContext.current
    val currentRoom by roomViewModel.currentRoom.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser // Obtener el usuario actual
    val coroutineScope = rememberCoroutineScope() // Necesario para lanzar corrutinas

    // Iniciar la observación de la sala cuando se entra a esta pantalla
    LaunchedEffect(roomId) {
        roomViewModel.observeRoom(roomId)
    }

    // Efecto para reaccionar a los cambios en la sala observada
    LaunchedEffect(currentRoom) {
        currentRoom?.let { room ->
            if (room.status == "playing") {
                // Si la sala está en estado "playing", significa que ambos jugadores están listos.
                // El host debe inicializar el juego si aún no lo ha hecho.
                if (room.hostId == currentUser?.uid && room.board.isEmpty()) {
                    // Solo el host inicializa el tablero la primera vez
                    coroutineScope.launch {
                        try {
                            room.guestId?.let { guestId ->
                                gameViewModel.initializeGame(room.roomId, room.hostId, guestId)
                                Log.d("WaitingRoom", "Juego inicializado por el host.")
                            } ?: Log.w("WaitingRoom", "GuestId es nulo al intentar inicializar juego.")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al inicializar juego: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                // Navegar a la pantalla del juego para ambos jugadores
                Toast.makeText(context, "¡Juego listo! Iniciando...", Toast.LENGTH_SHORT).show()
                navController.navigate("${navRutas.gameRoom}/$roomId") {
                    popUpTo(navRutas.waitingRoom) { inclusive = true } // Limpiar la pila
                }

            } else if (room.roomId.isEmpty() && room.hostId.isEmpty()) { // Room deleted from Firestore
                Toast.makeText(context, "La sala fue eliminada o ya no existe.", Toast.LENGTH_LONG).show()
                navController.navigate(navRutas.inicio) {
                    popUpTo(navRutas.inicio) { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sala de Espera") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ID de la Sala: $roomId",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            val hostEmail = currentRoom?.hostEmail ?: "Desconocido"
            val guestEmail = currentRoom?.guestEmail ?: "Nadie"

            Text(
                text = "Anfitrión: $hostEmail",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Jugador 2: $guestEmail",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (currentRoom?.guestId == null) {
                Text(
                    text = "Esperando que otro jugador se una...",
                    style = MaterialTheme.typography.headlineSmall
                )
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            } else {
                Text(
                    text = "¡Ambos jugadores listos! Iniciando juego...",
                    style = MaterialTheme.typography.headlineSmall
                )
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp)) // Mostrar progreso mientras se inicializa/navega
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = {
                // Si el host se va, podría eliminar la sala
                if (currentRoom?.hostId == currentUser?.uid) {
                    coroutineScope.launch {
                        try {
                            gameViewModel.deleteRoom(roomId)
                            Toast.makeText(context, "Sala eliminada.", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al eliminar sala: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                roomViewModel.resetCurrentRoom() // Limpiar el estado de la sala
                navController.navigate(navRutas.inicio) {
                    popUpTo(navRutas.inicio) { inclusive = true }
                }
            }) {
                Text("Volver al Inicio")
            }
        }
    }
}