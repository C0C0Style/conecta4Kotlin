package com.example.conecta4.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conecta4.data.model.Room
import com.example.conecta4.data.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

// Estados específicos para las operaciones de sala
sealed class RoomOperationState {
    object Idle : RoomOperationState()
    object Loading : RoomOperationState()
    data class Success(val message: String? = null, val roomId: String? = null, val room: Room? = null) : RoomOperationState()
    data class Error(val message: String) : RoomOperationState()
}

class RoomViewModel : ViewModel() {

    private val roomRepository: RoomRepository = RoomRepository()

    // Estado para operaciones de creación/unión de sala
    private val _roomOperationState = MutableStateFlow<RoomOperationState>(RoomOperationState.Idle)
    val roomOperationState: StateFlow<RoomOperationState> = _roomOperationState.asStateFlow()

    // Estado para la sala actual que se está observando
    private val _currentRoom = MutableStateFlow<Room?>(null)
    val currentRoom: StateFlow<Room?> = _currentRoom.asStateFlow()

    /**
     * Crea una nueva sala.
     */
    fun createRoom() {
        viewModelScope.launch {
            _roomOperationState.value = RoomOperationState.Loading
            try {
                // La lógica de creación de sala modificada para usar Map ya está en RoomRepository
                val roomId = roomRepository.createRoom()
                _roomOperationState.value = RoomOperationState.Success(message = "Sala creada con ID: $roomId", roomId = roomId)
                observeRoom(roomId) // Empezar a observar la sala recién creada
            } catch (e: Exception) {
                _roomOperationState.value = RoomOperationState.Error(e.message ?: "Error desconocido al crear sala.")
            }
        }
    }

    /**
     * Se une a una sala existente.
     * @param roomId El ID de la sala a la que unirse.
     */
    fun joinRoom(roomId: String) { // <--- ASEGÚRATE DE QUE ESTE PARÁMETRO ESTÉ PRESENTE
        viewModelScope.launch {
            _roomOperationState.value = RoomOperationState.Loading
            try {
                val room = roomRepository.joinRoom(roomId)
                _roomOperationState.value = RoomOperationState.Success(
                    message = "¡Te has unido a la sala ${room.roomId}!",
                    roomId = room.roomId,
                    room = room
                )
                // Iniciar la observación de la sala a la que se unió
                observeRoom(roomId)
            } catch (e: Exception) {
                _roomOperationState.value = RoomOperationState.Error(e.message ?: "Error desconocido al unirse a la sala.")
            }
        }
    }

    /**
     * Observa los cambios en una sala específica en tiempo real.
     */
    fun observeRoom(roomId: String) {
        viewModelScope.launch {
            roomRepository.getRoomUpdates(roomId).collect { room ->
                _currentRoom.value = room
                Log.d("RoomViewModel", "Actualización de sala: $room")
            }
        }
    }

    /**
     * Resetea el estado de operación de la sala.
     */
    fun resetRoomOperationState() {
        _roomOperationState.value = RoomOperationState.Idle
    }

    /**
     * Resetea la sala actual observada.
     */
    fun resetCurrentRoom() {
        _currentRoom.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Aquí podrías cancelar cualquier Flow o Coroutine si no se hace automáticamente con viewModelScope
    }
}