package com.example.conecta4.data.repository

import android.util.Log
import com.example.conecta4.data.model.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RoomRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Crea una nueva sala en Firestore.
     * @return El ID de la sala creada (código de sala).
     * @throws Exception si hay un error al crear la sala.
     */
    suspend fun createRoom(): String {
        val currentUser = auth.currentUser ?: throw IllegalStateException("Usuario no autenticado.")
        val userId = currentUser.uid
        val userEmail = currentUser.email ?: "Desconocido"

        val roomId = UUID.randomUUID().toString().substring(0, 6).uppercase()

        var newRoom = Room(
            roomId = roomId,
            hostId = userId,
            hostEmail = userEmail,
            status = "waiting"
        )
        newRoom.initializeBoard() // Aseguramos que 'board' es una lista 1D

        // --- INICIO DE LA MODIFICACIÓN ---
        // Convertir el objeto Room a un Map para un control explícito sobre la serialización
        val roomMap = mapOf(
            "roomId" to newRoom.roomId,
            "hostId" to newRoom.hostId,
            "hostEmail" to newRoom.hostEmail,
            "guestId" to newRoom.guestId,
            "guestEmail" to newRoom.guestEmail,
            "status" to newRoom.status,
            "createdAt" to newRoom.createdAt,
            "board" to newRoom.board, // Aquí, 'board' es definitivamente List<Int>
            "currentPlayerId" to newRoom.currentPlayerId,
            "winnerId" to newRoom.winnerId,
            "isDraw" to newRoom.isDraw,
            "lastMoveColumn" to newRoom.lastMoveColumn
        )
        // --- FIN DE LA MODIFICACIÓN ---

        return try {
            // Usar el Map en lugar del objeto Room directamente
            firestore.collection("rooms").document(roomId).set(roomMap).await()
            Log.d("RoomRepository", "Sala $roomId creada con éxito (usando Map).")
            roomId
        } catch (e: Exception) {
            Log.e("RoomRepository", "Error al crear sala (usando Map): ${e.message}")
            throw e
        }
    }

    /**
     * Intenta unirse a una sala existente.
     * @return La sala actualizada si el usuario se une exitosamente.
     * @throws Exception si la sala no existe, está llena, o ya está en juego.
     */
    suspend fun joinRoom(roomId: String): Room { // <--- CAMBIO AQUÍ: RETORNA Room
        val currentUser = auth.currentUser ?: throw IllegalStateException("Usuario no autenticado.")
        val userId = currentUser.uid
        val userEmail = currentUser.email ?: "Desconocido"

        if (roomId.isBlank()) {
            throw IllegalArgumentException("El ID de la sala no puede estar vacío.")
        }

        return try {
            val roomRef = firestore.collection("rooms").document(roomId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(roomRef)
                val room = snapshot.toObject(Room::class.java)
                    ?: throw NoSuchElementException("Sala con ID $roomId no encontrada.")

                if (room.guestId != null) {
                    throw IllegalStateException("La sala con ID $roomId ya está llena.")
                }

                if (room.hostId == userId) {
                    throw IllegalStateException("No puedes unirte a tu propia sala.")
                }

                val updatedRoomObject = room.copy(
                    guestId = userId,
                    guestEmail = userEmail,
                    status = "playing",
                    currentPlayerId = room.hostId // El anfitrión comienza el juego
                )

                val updatedRoomMap = mapOf(
                    "roomId" to updatedRoomObject.roomId,
                    "hostId" to updatedRoomObject.hostId,
                    "hostEmail" to updatedRoomObject.hostEmail,
                    "guestId" to updatedRoomObject.guestId,
                    "guestEmail" to updatedRoomObject.guestEmail,
                    "status" to updatedRoomObject.status,
                    "createdAt" to updatedRoomObject.createdAt,
                    "board" to updatedRoomObject.board,
                    "currentPlayerId" to updatedRoomObject.currentPlayerId,
                    "winnerId" to updatedRoomObject.winnerId,
                    "isDraw" to updatedRoomObject.isDraw,
                    "lastMoveColumn" to updatedRoomObject.lastMoveColumn
                )
                transaction.set(roomRef, updatedRoomMap)
                updatedRoomObject // <--- CAMBIO AQUÍ: RETORNA EL OBJETO Room
            }.await()
        } catch (e: Exception) {
            Log.e("RoomRepository", "Error al unirse a la sala $roomId: ${e.message}")
            throw e
        }
    }

    /**
     * Obtiene actualizaciones en tiempo real de una sala específica.
     * @param roomId El ID de la sala a observar.
     * @return Un Flow que emite el objeto Room cada vez que cambia.
     */
    fun getRoomUpdates(roomId: String) =
        firestore.collection("rooms").document(roomId)
            .snapshots()

    // Extensión para convertir un Snapshot de Firestore en un Flow de Room
    private fun com.google.firebase.firestore.DocumentReference.snapshots() =
        kotlinx.coroutines.flow.callbackFlow {
            val listenerRegistration = addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e) // Cierra el canal con la excepción
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(Room::class.java)) // Envía el objeto Room
                } else {
                    trySend(null) // Si el documento no existe o ha sido borrado
                }
            }
            awaitClose { listenerRegistration.remove() } // Cuando el Flow se cancela, quita el listener
        }
}