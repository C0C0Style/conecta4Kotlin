package com.example.conecta4.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conecta4.data.model.Room
import com.example.conecta4.data.repository.GameRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

// Estados específicos para el juego
sealed class GameState {
    object Loading : GameState()
    data class Active(val room: Room, val board2D: List<List<Int>>) : GameState() // Añadir board2D aquí
    data class GameOver(val room: Room, val message: String, val board2D: List<List<Int>>) : GameState() // Añadir board2D
    data class Error(val message: String) : GameState()
    object Idle : GameState()
}

class GameViewModel : ViewModel() {

    private val gameRepository: GameRepository = GameRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _gameState = MutableStateFlow<GameState>(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    // Constantes para el tablero
    private val NUM_ROWS = 6
    private val NUM_COLS = 7

    /**
     * Inicia la observación de una sala específica para el juego.
     */
    fun startGameObservation(roomId: String) {
        _gameState.value = GameState.Loading
        viewModelScope.launch {
            gameRepository.getRoomUpdates(roomId).collect { room ->
                if (room == null) {
                    _gameState.value = GameState.Error("La sala ya no existe o ha sido eliminada.")
                    return@collect
                }

                // Obtener la representación 2D del tablero de la sala
                val currentBoard2D = room.getBoard2D()

                when (room.status) {
                    "playing" -> {
                        if (room.winnerId == null && !room.isDraw) {
                            _gameState.value = GameState.Active(room, currentBoard2D) // Pasar board2D
                        } else {
                            val message = if (room.winnerId != null) {
                                if (room.winnerId == currentUserId) "¡Has ganado!" else "¡Has perdido!"
                            } else if (room.isDraw) {
                                "¡Empate!"
                            } else {
                                "Juego terminado."
                            }
                            _gameState.value = GameState.GameOver(room, message, currentBoard2D) // Pasar board2D
                        }
                    }
                    "waiting" -> {
                        Log.w("GameViewModel", "Sala $roomId en estado 'waiting' en GameScreen.")
                        _gameState.value = GameState.Error("La sala aún está esperando jugadores.")
                    }
                    "finished" -> {
                        val message = if (room.winnerId != null) {
                            if (room.winnerId == currentUserId) "¡Has ganado!" else "¡Has perdido!"
                        } else if (room.isDraw) {
                            "¡Empate!"
                        } else {
                            "Juego terminado."
                        }
                        _gameState.value = GameState.GameOver(room, message, currentBoard2D) // Pasar board2D
                    }
                    else -> {
                        _gameState.value = GameState.Error("Estado de sala desconocido: ${room.status}")
                    }
                }
            }
        }
    }

    /**
     * Inicializa el estado del juego en la base de datos a través del repositorio.
     * Esta función es llamada por WaitingRoomScreen.
     */
    suspend fun initializeGame(roomId: String, hostId: String, guestId: String) {
        try {
            gameRepository.initializeGame(roomId, hostId, guestId)
            Log.d("GameViewModel", "initializeGame: Juego inicializado en repo para sala $roomId")
        } catch (e: Exception) {
            Log.e("GameViewModel", "initializeGame: Error al inicializar juego: ${e.message}")
            throw e
        }
    }

    /**
     * Intenta hacer un movimiento en la columna especificada.
     * Solo permite el movimiento si es el turno del jugador actual y la columna es válida.
     */
    fun makeMove(roomId: String, column: Int) {
        val currentState = _gameState.value
        if (currentState !is GameState.Active) {
            Log.w("GameViewModel", "No se puede hacer un movimiento: el juego no está activo.")
            return
        }

        val room = currentState.room
        // Obtener la representación 2D para la lógica del juego
        val board2D = room.getBoard2D().map { it.toMutableList() }.toMutableList()

        val currentPlayerId = auth.currentUser?.uid

        // 1. Verificar si es el turno del jugador actual
        if (currentPlayerId != room.currentPlayerId) {
            Log.w("GameViewModel", "No es tu turno.")
            return
        }

        // 2. Verificar si la columna es válida y no está llena
        if (column < 0 || column >= NUM_COLS || board2D[0][column] != 0) {
            Log.w("GameViewModel", "Columna inválida o llena.")
            return
        }

        // 3. Encontrar la fila más baja disponible
        var row = -1
        for (i in NUM_ROWS - 1 downTo 0) {
            if (board2D[i][column] == 0) {
                row = i
                break
            }
        }

        if (row == -1) {
            Log.w("GameViewModel", "Columna llena.")
            return
        }

        // 4. Realizar el movimiento
        val playerPiece = if (currentPlayerId == room.hostId) 1 else 2
        board2D[row][column] = playerPiece

        // 5. Verificar si hay un ganador o empate
        var winnerId: String? = null
        var isDraw = false

        if (checkWin(board2D, playerPiece)) {
            winnerId = currentPlayerId
            Log.d("GameViewModel", "¡Ganador: $winnerId!")
        } else if (isBoardFull(board2D)) {
            isDraw = true
            Log.d("GameViewModel", "¡Empate!")
        }

        // 6. Determinar el siguiente jugador
        val nextPlayer = if (winnerId != null || isDraw) {
            "" // No hay siguiente jugador si el juego ha terminado
        } else if (currentPlayerId == room.hostId) {
            room.guestId
        } else {
            room.hostId
        }

        // 7. Actualizar el estado del juego en Firestore
        viewModelScope.launch {
            try {
                // Convertir el tablero 2D a 1D antes de enviarlo al repositorio
                val newBoard1D = Room.convertBoard2DTo1D(board2D)
                gameRepository.makeMove(room.roomId, newBoard1D, nextPlayer ?: "", winnerId, isDraw, column)
            } catch (e: Exception) {
                _gameState.value = GameState.Error("Error al enviar movimiento: ${e.message}")
            }
        }
    }

    // --- Lógica de verificación de Conecta 4 (usa la representación 2D) ---
    private fun checkWin(board: List<List<Int>>, player: Int): Boolean {
        // Check horizontal
        for (r in 0 until NUM_ROWS) {
            for (c in 0 until NUM_COLS - 3) {
                if (board[r][c] == player && board[r][c + 1] == player &&
                    board[r][c + 2] == player && board[r][c + 3] == player) {
                    return true
                }
            }
        }

        // Check vertical
        for (r in 0 until NUM_ROWS - 3) {
            for (c in 0 until NUM_COLS) {
                if (board[r][c] == player && board[r + 1][c] == player &&
                    board[r + 2][c] == player && board[r + 3][c] == player) {
                    return true
                }
            }
        }

        // Check diagonal (top-left to bottom-right)
        for (r in 0 until NUM_ROWS - 3) {
            for (c in 0 until NUM_COLS - 3) {
                if (board[r][c] == player && board[r + 1][c + 1] == player &&
                    board[r + 2][c + 2] == player && board[r + 3][c + 3] == player) {
                    return true
                }
            }
        }

        // Check diagonal (top-right to bottom-left)
        for (r in 3 until NUM_ROWS) {
            for (c in 0 until NUM_COLS - 3) {
                if (board[r][c] == player && board[r - 1][c + 1] == player &&
                    board[r - 2][c + 2] == player && board[r - 3][c + 3] == player) {
                    return true
                }
            }
        }
        return false
    }

    private fun isBoardFull(board: List<List<Int>>): Boolean {
        for (r in board.indices) {
            for (c in board[r].indices) {
                if (board[r][c] == 0) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Reinicia el estado del ViewModel a Idle.
     */
    fun resetGameState() {
        _gameState.value = GameState.Idle
    }

    /**
     * Elimina la sala de Firestore (útil para el host al finalizar el juego).
     */
    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            try {
                gameRepository.deleteRoom(roomId)
                Log.d("GameViewModel", "Sala $roomId marcada para eliminación.")
                _gameState.value = GameState.Idle // Volver a un estado inactivo
            } catch (e: Exception) {
                _gameState.value = GameState.Error("Error al eliminar sala: ${e.message}")
            }
        }
    }
}