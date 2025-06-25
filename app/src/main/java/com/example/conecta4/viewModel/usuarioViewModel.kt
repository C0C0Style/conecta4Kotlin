package com.example.conecta4.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.conecta4.data.repository.UsuarioRepository // Importamos nuestro Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log // Para mensajes de depuración

// Definimos los posibles estados de la operación de autenticación
sealed class AuthState {
    object Idle : AuthState()       // Estado inicial, o después de un éxito/error manejado
    object Loading : AuthState()    // Operación en curso (registro o login)
    object Success : AuthState()    // Operación completada con éxito
    data class Error(val message: String) : AuthState() // Error con un mensaje descriptivo
}

class UsuarioViewModel : ViewModel() {

    // Instanciamos el Repository directamente aquí. Esto es lo más sencillo sin DI.
    private val usuarioRepository: UsuarioRepository = UsuarioRepository()

    // MutableStateFlow para que el ViewModel pueda cambiar el estado.
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    // StateFlow para que la UI pueda observar el estado de forma segura.
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Inicia el proceso de registro de un nuevo usuario.
     * Actualiza el 'authState' según el resultado.
     */
    fun registerUser(email: String, password: String) {
        // Lanzamos una corrutina para ejecutar la operación asíncrona.
        viewModelScope.launch {
            _authState.value = AuthState.Loading // Indicamos que estamos cargando.
            try {
                usuarioRepository.registerUser(email, password)
                _authState.value = AuthState.Success // Si todo va bien, éxito.
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Error desconocido al registrar."
                _authState.value = AuthState.Error(errorMessage) // Si hay un error, lo enviamos.
            }
        }
    }

    /**
     * Inicia el proceso de inicio de sesión.
     * Actualiza el 'authState' según el resultado.
     */
    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading // Indicamos que estamos cargando.
            try {
                usuarioRepository.loginUser(email, password)
                _authState.value = AuthState.Success // Si todo va bien, éxito.
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Error desconocido al iniciar sesión."
                _authState.value = AuthState.Error(errorMessage) // Si hay un error, lo enviamos.
            }
        }
    }
    fun logoutUser() {
        viewModelScope.launch {
            try {
                usuarioRepository.logoutUser()
                _authState.value = AuthState.Idle // Volvemos al estado inactivo
                Log.d("UsuarioViewModel", "Sesión cerrada correctamente. AuthState reseteado a Idle.")
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Error desconocido al cerrar sesión."
                _authState.value = AuthState.Error(errorMessage) // Informamos de cualquier error
                Log.e("UsuarioViewModel", "Fallo al cerrar sesión: $errorMessage")
            }
        }
    }

    /**
     * Resetea el estado de autenticación a 'Idle'.
     * Esto es importante para que la UI pueda reaccionar a un nuevo intento o para limpiar mensajes.
     */
    fun resetAuthState() {
        _authState.value = AuthState.Idle
        Log.d("UsuarioViewModel", "Estado de autenticación reseteado a Idle.")
    }
}