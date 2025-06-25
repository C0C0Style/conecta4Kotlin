package com.example.conecta4.data.model

data class Room(
    val roomId: String = "",
    val hostId: String = "",
    val hostEmail: String = "",
    var guestId: String? = null,
    var guestEmail: String? = null,
    var status: String = "waiting", // "waiting", "playing", "finished"
    val createdAt: Long = System.currentTimeMillis(),

    // --- Propiedad 'board' modificada para ser una lista 1D ---
    var board: List<Int> = emptyList(), // Representación del tablero plano (0: vacío, 1: jugador 1, 2: jugador 2)
    var currentPlayerId: String? = null,
    var winnerId: String? = null,
    var isDraw: Boolean = false,
    var lastMoveColumn: Int = -1
) {
    // Constructor sin argumentos para Firebase Firestore (es necesario)
    constructor() : this("", "", "")

    // Función para inicializar un tablero vacío de Conecta 4 (6 filas * 7 columnas = 42 celdas)
    fun initializeBoard() {
        board = List(6 * 7) { 0 } // 6 filas, 7 columnas, todo a 0 (vacío)
    }

    // --- Nuevas funciones de ayuda para convertir entre 1D y 2D ---
    fun getBoard2D(): List<List<Int>> {
        val numRows = 6
        val numCols = 7
        return List(numRows) { row ->
            List(numCols) { col ->
                board[row * numCols + col]
            }
        }
    }

    companion object {
        fun convertBoard2DTo1D(board2D: List<List<Int>>): List<Int> {
            val flattenedList = mutableListOf<Int>()
            for (row in board2D) {
                flattenedList.addAll(row)
            }
            return flattenedList
        }
    }
}