package com.example.conecta4.view

// package com.example.conecta4.view

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
import com.example.conecta4.viewModel.RoomOperationState
import com.example.conecta4.viewModel.RoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewCreateRoom(
    navController: NavController,
    roomViewModel: RoomViewModel = viewModel() // Obtener el RoomViewModel
) {
    val context = LocalContext.current
    val roomOperationState by roomViewModel.roomOperationState.collectAsState()
    val currentRoom by roomViewModel.currentRoom.collectAsState() // Observar la sala actual

    // LaunchedEffect para manejar el estado de la operación
    LaunchedEffect(roomOperationState) {
        when (roomOperationState) {
            is RoomOperationState.Success -> {
                val successState = roomOperationState as RoomOperationState.Success
                successState.message?.let { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
                successState.roomId?.let { roomId ->
                    // Navegar a una pantalla de espera o a la pantalla del juego
                    // pasando el roomId para que la otra pantalla pueda observar la sala.
                    navController.navigate("${navRutas.waitingRoom}/$roomId")
                }
                roomViewModel.resetRoomOperationState() // Resetear el estado
            }
            is RoomOperationState.Error -> {
                val errorState = roomOperationState as RoomOperationState.Error
                Toast.makeText(context, "Error: ${errorState.message}", Toast.LENGTH_LONG).show()
                roomViewModel.resetRoomOperationState() // Resetear el estado
            }
            else -> { /* Idle o Loading, no hacer nada aquí */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Crear Sala") })
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
                text = "Crea una nueva sala para jugar con un amigo.",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            ElevatedButton(
                onClick = { roomViewModel.createRoom() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = roomOperationState != RoomOperationState.Loading
            ) {
                Text(if (roomOperationState is RoomOperationState.Loading) "Creando..." else "Crear Sala")
            }

            if (roomOperationState is RoomOperationState.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }

            // Opcional: Mostrar el código de la sala si ya se creó y estás esperando al segundo jugador
            if (currentRoom?.roomId != null && currentRoom?.guestId == null && currentRoom?.status == "waiting") {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Código de Sala: ${currentRoom?.roomId}",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Esperando a otro jugador...",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}