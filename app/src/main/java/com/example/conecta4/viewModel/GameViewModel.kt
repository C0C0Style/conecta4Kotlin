package com.example.conecta4.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conecta4.data.model.Room
import com.example.conecta4.data.repository.GameRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException // Importar CancellationException

sealed class GameState {
    object Loading : GameState()
    object Idle : GameState()
    data class Active(val room: Room, val board2D: List<List<Int>>) : GameState()
    data class GameOver(val message: String, val room: Room, val board2D: List<List<Int>>) : GameState()
    data class Error(val message: String) : GameState()
    data class AwaitingTranslation(val room: Room, val board2D: List<List<Int>>, val englishWord: String, val timeLeft: Int) : GameState()
}

class GameViewModel(
    private val gameRepository: GameRepository = GameRepository()
) : ViewModel() {

    private val _gameState = MutableStateFlow<GameState>(GameState.Loading)
    val gameState: StateFlow<GameState> = _gameState

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

    private val words = arrayOf(
        "Hello" to "Hola",
        "World" to "Mundo",
        "Cat" to "Gato",
        "Dog" to "Perro",
        "House" to "Casa",
        "Tree" to "Arbol",
        "Water" to "Agua",
        "Fire" to "Fuego",
        "Sun" to "Sol",
        "Moon" to "Luna",
        "Bird" to "Pajaro",
        "Fish" to "Pez",
        "Car" to "Carro",
        "Book" to "Libro",
        "Computer" to "Computador",
        "Phone" to "Telefono",
        "Friend" to "Amigo",
        "Family" to "Familia",
        "Happy" to "Feliz",
        "Sad" to "Triste"
    )
    private var currentWordPair: Pair<String, String>? = null
    private var translationTimerJob: Job? = null
    private val _timeLeft = MutableStateFlow(0)
    val timeLeft: StateFlow<Int> = _timeLeft

    fun startGameObservation(roomId: String) {
        viewModelScope.launch {
            gameRepository.getRoomUpdates(roomId).collect { room ->
                if (room == null) {
                    _gameState.value = GameState.Idle
                    translationTimerJob?.cancel()
                    return@collect
                }

                val board2D = room.getBoard2D()

                if (room.winnerId != null) {
                    val winnerMessage = when (room.winnerId) {
                        "draw" -> "¡Empate!"
                        else -> {
                            val winnerEmail = if (room.winnerId == room.hostId) room.hostEmail else room.guestEmail
                            "¡${winnerEmail?.substringBefore('@')} ha ganado!"
                        }
                    }
                    _gameState.value = GameState.GameOver(winnerMessage, room, board2D)
                    translationTimerJob?.cancel()
                } else {
                    if (room.currentPlayerId == currentUserId && room.status == "playing") {
                        if (_gameState.value !is GameState.AwaitingTranslation) {
                            Log.d("GameViewModel", "Es mi turno y no hay pregunta activa, iniciando pregunta de traducción.")
                            startTranslationQuestion(room, board2D)
                        } else {
                            val currentState = _gameState.value as GameState.AwaitingTranslation
                            _gameState.value = currentState.copy(room = room, board2D = board2D, timeLeft = _timeLeft.value)
                        }
                    } else if (room.currentPlayerId != currentUserId && room.status == "playing") {
                        if (_gameState.value is GameState.AwaitingTranslation) {
                            translationTimerJob?.cancel()
                            Log.d("GameViewModel", "Ya no es mi turno, cancelando temporizador de traducción.")
                        }
                        _gameState.value = GameState.Active(room, board2D)
                    } else if (room.status == "waiting") {
                        _gameState.value = GameState.Idle
                    }
                }
            }
        }
    }

    fun startTranslationQuestion(room: Room, board2D: List<List<Int>>) {
        currentWordPair = words.random()
        val englishWord = currentWordPair!!.first
        _timeLeft.value = 3 // Puedes cambiar este valor para más tiempo

        _gameState.value = GameState.AwaitingTranslation(room, board2D, englishWord, _timeLeft.value)

        translationTimerJob?.cancel()
        translationTimerJob = viewModelScope.launch(Dispatchers.Main) {
            for (i in 10  downTo 0) { // Y este '3' también
                _timeLeft.value = i
                if (i > 0) delay(1000)
            }
            if (_gameState.value is GameState.AwaitingTranslation && currentWordPair != null &&
                ( _gameState.value as GameState.AwaitingTranslation).room.currentPlayerId == currentUserId) {
                Log.d("GameViewModel", "¡Tiempo agotado para $currentUserId! Pasando el turno.")
                val nextPlayerId = if (room.currentPlayerId == room.hostId) room.guestId else room.hostId

                // MODIFICACIÓN AQUI: Asegurarse de que makeMove se lance en viewModelScope
                viewModelScope.launch {
                    try {
                        gameRepository.makeMove(
                            roomId = room.roomId,
                            newBoard1D = room.board,
                            nextPlayerId = nextPlayerId!!,
                            winnerId = null,
                            isDraw = false,
                            lastMoveCol = -1
                        )
                    } catch (e: CancellationException) {
                        // Es una cancelación, lo tratamos como una advertencia
                        Log.w("GameViewModel", "Operación makeMove (tiempo agotado) cancelada: ${e.message}")
                    } catch (e: Exception) {
                        _gameState.value = GameState.Error("Error al pasar el turno por tiempo: ${e.message}")
                    }
                }
                currentWordPair = null
            }
        }
    }

    fun checkTranslationAnswer(roomId: String, playerAnswer: String) {
        translationTimerJob?.cancel()

        if (currentWordPair != null) {
            val (english, spanish) = currentWordPair!!
            if (playerAnswer.trim().equals(spanish, ignoreCase = true)) {
                Log.d("GameViewModel", "Respuesta correcta para $currentUserId. Permitiendo movimiento.")
                val currentRoom = (_gameState.value as? GameState.AwaitingTranslation)?.room
                val currentBoard = (_gameState.value as? GameState.AwaitingTranslation)?.board2D
                if (currentRoom != null && currentBoard != null) {
                    _gameState.value = GameState.Active(currentRoom, currentBoard)
                }
            } else {
                Log.d("GameViewModel", "Respuesta incorrecta para $currentUserId. Pasando el turno.")
                val currentRoom = (_gameState.value as? GameState.AwaitingTranslation)?.room
                if (currentRoom != null) {
                    val nextPlayerId = if (currentRoom.currentPlayerId == currentRoom.hostId) currentRoom.guestId else currentRoom.hostId
                    viewModelScope.launch {
                        try {
                            gameRepository.makeMove(
                                roomId = currentRoom.roomId,
                                newBoard1D = currentRoom.board,
                                nextPlayerId = nextPlayerId!!,
                                winnerId = null,
                                isDraw = false,
                                lastMoveCol = -1
                            )
                        } catch (e: CancellationException) {
                            // Es una cancelación, lo tratamos como una advertencia
                            Log.w("GameViewModel", "Operación makeMove (respuesta incorrecta) cancelada: ${e.message}")
                        } catch (e: Exception) {
                            _gameState.value = GameState.Error("Error al pasar el turno por respuesta incorrecta: ${e.message}")
                        }
                    }
                }
            }
            currentWordPair = null
        }
    }
    suspend fun initializeGame(roomId: String, hostId: String, guestId: String) {
        try {
            gameRepository.initializeGame(roomId, hostId, guestId)
            Log.d("GameViewModel", "initializeGame: Juego inicializado en repo para sala $roomId")
        } catch (e: Exception) {
            Log.e("GameViewModel", "initializeGame: Error al inicializar juego: ${e.message}")
            throw e
        }
    }

    fun makeMove(roomId: String, column: Int) {
        if (_gameState.value !is GameState.Active) {
            Log.w("GameViewModel", "Intento de movimiento en estado incorrecto: ${_gameState.value}")
            return
        }

        val currentState = _gameState.value as GameState.Active
        val room = currentState.room

        if (room.currentPlayerId != currentUserId) {
            Log.w("GameViewModel", "No es tu turno para mover.")
            _gameState.value = GameState.Error("No es tu turno.")
            return
        }
        if (room.winnerId != null) {
            Log.w("GameViewModel", "El juego ya ha terminado.")
            _gameState.value = GameState.Error("El juego ya ha terminado.")
            return
        }

        viewModelScope.launch {
            try {
                val currentBoard2D = room.getBoard2D().toMutableList().map { it.toMutableList() }

                val rows = 6
                val columns = 7
                val playerPiece = if (room.currentPlayerId == room.hostId) 1 else 2

                var rowIndex = -1
                for (r in rows - 1 downTo 0) {
                    if (currentBoard2D[r][column] == 0) {
                        currentBoard2D[r][column] = playerPiece
                        rowIndex = r
                        break
                    }
                }

                if (rowIndex == -1) {
                    _gameState.value = GameState.Error("Columna llena. Elige otra.")
                    return@launch
                }

                val newBoard1D = Room.convertBoard2DTo1D(currentBoard2D)

                val winner = checkWinner(newBoard1D, rows, columns, playerPiece)
                val isBoardFull = newBoard1D.none { it == 0 }

                val nextPlayerId = if (winner || isBoardFull) {
                    room.currentPlayerId
                } else {
                    if (room.currentPlayerId == room.hostId) room.guestId else room.hostId
                }

                gameRepository.makeMove(
                    roomId = roomId,
                    newBoard1D = newBoard1D,
                    nextPlayerId = nextPlayerId!!,
                    winnerId = if (winner) currentUserId else if (isBoardFull) "draw" else null,
                    isDraw = isBoardFull && !winner,
                    lastMoveCol = column
                )
            } catch (e: CancellationException) {
                // Es una cancelación, lo tratamos como una advertencia
                Log.w("GameViewModel", "Operación makeMove (movimiento de usuario) cancelada: ${e.message}")
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error al hacer el movimiento: ${e.message}")
                _gameState.value = GameState.Error("Error al hacer el movimiento: ${e.message}")
            }
        }
    }

    private fun checkWinner(board1D: List<Int>, rows: Int, columns: Int, playerPiece: Int): Boolean {
        val board2D = List(rows) { rowIndex ->
            List(columns) { colIndex ->
                board1D[rowIndex * columns + colIndex]
            }
        }

        for (r in 0 until rows) {
            for (c in 0..columns - 4) {
                if (board2D[r][c] == playerPiece &&
                    board2D[r][c + 1] == playerPiece &&
                    board2D[r][c + 2] == playerPiece &&
                    board2D[r][c + 3] == playerPiece
                ) return true
            }
        }

        for (c in 0 until columns) {
            for (r in 0..rows - 4) {
                if (board2D[r][c] == playerPiece &&
                    board2D[r + 1][c] == playerPiece &&
                    board2D[r + 2][c] == playerPiece &&
                    board2D[r + 3][c] == playerPiece
                ) return true
            }
        }

        for (r in 0..rows - 4) {
            for (c in 0..columns - 4) {
                if (board2D[r][c] == playerPiece &&
                    board2D[r + 1][c + 1] == playerPiece &&
                    board2D[r + 2][c + 2] == playerPiece &&
                    board2D[r + 3][c + 3] == playerPiece
                ) return true
            }
        }

        for (r in 3 until rows) {
            for (c in 0..columns - 4) {
                if (board2D[r][c] == playerPiece &&
                    board2D[r - 1][c + 1] == playerPiece &&
                    board2D[r - 2][c + 2] == playerPiece &&
                    board2D[r - 3][c + 3] == playerPiece
                ) return true
            }
        }
        return false
    }

    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            try {
                gameRepository.deleteRoom(roomId)
                Log.d("GameViewModel", "Sala $roomId eliminada con éxito.")
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error al eliminar sala $roomId: ${e.message}")
                _gameState.value = GameState.Error("Error al eliminar sala: ${e.message}")
            }
        }
    }

    fun resetGameState() {
        _gameState.value = GameState.Loading
        translationTimerJob?.cancel()
        currentWordPair = null
    }

    override fun onCleared() {
        super.onCleared()
        translationTimerJob?.cancel()
    }
}