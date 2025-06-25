package com.example.conecta4.view

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
fun ViewJoinRoom(
    navController: NavController,
    roomViewModel: RoomViewModel = viewModel() // Obtener el RoomViewModel
) {
    var roomIdInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val roomOperationState by roomViewModel.roomOperationState.collectAsState()

    // LaunchedEffect para manejar el estado de la operación
    LaunchedEffect(roomOperationState) {
        when (roomOperationState) {
            is RoomOperationState.Success -> {
                val successState = roomOperationState as RoomOperationState.Success
                successState.message?.let { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
                successState.roomId?.let { roomId ->
                    // Navegar a la pantalla del juego (o sala de espera si aún no ha iniciado)
                    // Si el estado de la sala ya es "playing", se iría directamente al juego.
                    navController.navigate("${navRutas.gameRoom}/$roomId")
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
            TopAppBar(title = { Text("Unirse a Sala") })
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
                text = "Introduce el código de la sala para unirte a un juego existente.",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = roomIdInput,
                onValueChange = { roomIdInput = it.uppercase() }, // Convertir a mayúsculas
                label = { Text("Código de Sala") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            ElevatedButton(
                onClick = { roomViewModel.joinRoom(roomIdInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = roomIdInput.isNotBlank() && roomOperationState != RoomOperationState.Loading
            ) {
                Text(if (roomOperationState is RoomOperationState.Loading) "Uniéndote..." else "Unirse a Sala")
            }

            if (roomOperationState is RoomOperationState.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}