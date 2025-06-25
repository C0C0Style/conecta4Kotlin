package com.example.conecta4.data.repository

import android.util.Log
import com.example.conecta4.data.model.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GameRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Inicializa el estado del juego para una sala específica en Firestore.
     * Esto se llama cuando la sala pasa a estado "playing".
     */
    suspend fun initializeGame(roomId: String, hostId: String, guestId: String) {
        try {
            val roomRef = firestore.collection("rooms").document(roomId)
            val room = roomRef.get().await().toObject(Room::class.java)

            room?.let {
                it.initializeBoard() // Inicializa el tablero como una lista 1D
                // Decide quién empieza: el host por defecto, o aleatoriamente
                it.currentPlayerId = hostId // El host siempre empieza por defecto
                it.winnerId = null
                it.isDraw = false
                it.lastMoveColumn = -1
                it.status = "playing" // Asegurarse de que el estado es "playing"

                roomRef.set(it).await() // Actualiza la sala con el estado inicial del juego (board ya es 1D)
                Log.d("GameRepository", "Juego inicializado para sala $roomId. Turno de ${it.currentPlayerId}")
            } ?: throw NoSuchElementException("Sala con ID $roomId no encontrada para inicializar juego.")
        } catch (e: Exception) {
            Log.e("GameRepository", "Error al inicializar juego para sala $roomId: ${e.message}")
            throw e
        }
    }

    /**
     * Actualiza el estado del tablero (1D) y el turno en Firestore después de un movimiento.
     */
    suspend fun makeMove(roomId: String, newBoard1D: List<Int>, nextPlayerId: String, winnerId: String? = null, isDraw: Boolean = false, lastMoveCol: Int) {
        try {
            val roomRef = firestore.collection("rooms").document(roomId)
            roomRef.update(
                "board", newBoard1D, // Guardamos la lista 1D directamente
                "currentPlayerId", nextPlayerId,
                "winnerId", winnerId,
                "isDraw", isDraw,
                "lastMoveColumn", lastMoveCol,
                "status", if (winnerId != null || isDraw) "finished" else "playing"
            ).await()
            Log.d("GameRepository", "Movimiento realizado en sala $roomId. Turno de $nextPlayerId")
        } catch (e: Exception) {
            Log.e("GameRepository", "Error al realizar movimiento en sala $roomId: ${e.message}")
            throw e
        }
    }

    /**
     * Observa los cambios en el estado de una sala (incluyendo el juego) en tiempo real.
     */
    fun getRoomUpdates(roomId: String): Flow<Room?> =
        firestore.collection("rooms").document(roomId)
            .snapshots()

    // Extensión para convertir un DocumentReference de Firestore en un Flow de Room
    private fun com.google.firebase.firestore.DocumentReference.snapshots() =
        callbackFlow {
            val listenerRegistration = addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(Room::class.java))
                } else {
                    trySend(null)
                }
            }
            awaitClose { listenerRegistration.remove() }
        }

    /**
     * Elimina una sala de Firestore (ej. cuando el juego termina o un jugador se va).
     */
    suspend fun deleteRoom(roomId: String) {
        try {
            firestore.collection("rooms").document(roomId).delete().await()
            Log.d("GameRepository", "Sala $roomId eliminada de Firestore.")
        } catch (e: Exception) {
            Log.e("GameRepository", "Error al eliminar sala $roomId: ${e.message}")
            throw e
        }
    }
}