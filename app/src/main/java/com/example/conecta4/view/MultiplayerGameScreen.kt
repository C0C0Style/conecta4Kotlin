package com.example.conecta4.view

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.conecta4.util.navRutas
import com.example.conecta4.viewModel.GameState
import com.example.conecta4.viewModel.GameViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.Canvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerGameScreen(
    navController: NavController,
    roomId: String,
    gameViewModel: GameViewModel = viewModel()
) {
    val context = LocalContext.current
    val gameState by gameViewModel.gameState.collectAsState()
    val currentUserId = gameViewModel.currentUserId

    LaunchedEffect(roomId) {
        gameViewModel.startGameObservation(roomId)
    }

    LaunchedEffect(gameState) {
        when (gameState) {
            is GameState.Error -> {
                val errorMessage = (gameState as GameState.Error).message
                Toast.makeText(context, "Error en el juego: $errorMessage", Toast.LENGTH_LONG).show()
            }
            is GameState.GameOver -> {
                val gameOverState = gameState as GameState.GameOver
                Toast.makeText(context, gameOverState.message, Toast.LENGTH_LONG).show()
            }
            else -> { /* No hacer nada para Loading, Active, Idle en este LaunchedEffect */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conecta 4 Multijugador") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (gameState is GameState.Active) {
                            val room = (gameState as GameState.Active).room
                            if (room.hostId == currentUserId) {
                                gameViewModel.deleteRoom(roomId)
                            }
                        } else if (gameState is GameState.GameOver) {
                            val room = (gameState as GameState.GameOver).room
                            if (room.hostId == currentUserId) {
                                gameViewModel.deleteRoom(roomId)
                            }
                        }
                        gameViewModel.resetGameState()
                        navController.navigate(navRutas.inicio) {
                            popUpTo(navRutas.inicio) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
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
            when (gameState) {
                GameState.Loading -> {
                    CircularProgressIndicator()
                    Text("Cargando juego...")
                }
                GameState.Idle -> {
                    Text("Juego inactivo. ¿Problema de conexión o sala eliminada?")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        navController.navigate(navRutas.inicio) {
                            popUpTo(navRutas.inicio) { inclusive = true }
                        }
                    }) {
                        Text("Volver al Inicio")
                    }
                }
                is GameState.Active -> {
                    val room = (gameState as GameState.Active).room
                    val board = (gameState as GameState.Active).board2D // <-- Acceder al board2D
                    val currentPlayerIsMe = room.currentPlayerId == currentUserId

                    Text(
                        text = if (currentPlayerIsMe) "¡Es tu turno!" else "Turno de ${if (room.currentPlayerId == room.hostId) room.hostEmail?.substringBefore('@') else room.guestEmail?.substringBefore('@')}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (currentPlayerIsMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Board(board = board) { column ->
                        if (currentPlayerIsMe) {
                            gameViewModel.makeMove(roomId, column)
                        } else {
                            Toast.makeText(context, "No es tu turno.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                is GameState.GameOver -> {
                    val gameOverState = gameState as GameState.GameOver
                    val room = gameOverState.room
                    val board = gameOverState.board2D // <-- Acceder al board2D

                    Text(
                        text = gameOverState.message,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (room.winnerId == currentUserId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Board(board = board) { /* No clickable after game over */ }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = {
                        if (room.hostId == currentUserId) {
                            gameViewModel.deleteRoom(roomId)
                        }
                        gameViewModel.resetGameState()
                        navController.navigate(navRutas.inicio) {
                            popUpTo(navRutas.inicio) { inclusive = true }
                        }
                    }) {
                        Text("Volver al Inicio")
                    }
                }
                is GameState.Error -> {
                    val errorMessage = (gameState as GameState.Error).message
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ocurrió un error: $errorMessage", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            gameViewModel.resetGameState()
                            navController.navigate(navRutas.inicio) {
                                popUpTo(navRutas.inicio) { inclusive = true }
                            }
                        }) {
                            Text("Volver al Inicio")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Board(board: List<List<Int>>, onColumnClick: (Int) -> Unit) { // <-- Board sigue esperando List<List<Int>>
    Column(
        modifier = Modifier
            .background(Color.Blue)
            .padding(4.dp)
    ) {
        board.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                row.forEachIndexed { colIndex, cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .background(Color.White, CircleShape)
                            .clickable { onColumnClick(colIndex) },
                        contentAlignment = Alignment.Center
                    ) {
                        val pieceColor = when (cell) {
                            1 -> Color.Red
                            2 -> Color.Yellow
                            else -> Color.Transparent
                        }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = pieceColor, radius = size.minDimension / 2)
                        }
                    }
                }
            }
        }
    }
}