// Archivo: com.example.conecta4.viewModel/usuarioViewModel.kt

package com.example.conecta4.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Define los posibles estados de la operación de autenticación
sealed class AuthState {
    object Idle : AuthState() // Estado inicial o cuando no hay operación en curso
    object Loading : AuthState() // Operación en progreso
    object Success : AuthState() // Operación exitosa (registro o login)
    data class Error(val message: String) : AuthState() // Operación fallida con mensaje de error
}

class UsuarioViewModel(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) : ViewModel() {

    // _authState es MutableStateFlow para que el ViewModel pueda cambiarlo
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    // authState es StateFlow para que la Vista solo pueda leerlo (inmutable)
    val authState = _authState.asStateFlow()

    fun registerUser(email: String, password: String) {
        // Validación básica (puedes añadir más si es necesario)
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Correo y contraseña no pueden estar vacíos.")
            return
        }
        if (password.length < 6) { // Firebase requiere al menos 6 caracteres para la contraseña
            _authState.value = AuthState.Error("La contraseña debe tener al menos 6 caracteres.")
            return
        }

        _authState.value = AuthState.Loading // Cambia el estado a cargando

        viewModelScope.launch { // Ejecuta la operación en un ámbito de corrutina del ViewModel
            try {
                // Realiza la llamada a Firebase para crear el usuario
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            _authState.value = AuthState.Success // Si es exitoso, actualiza el estado a Success
                        } else {
                            // Si falla, actualiza el estado a Error con el mensaje
                            val errorMessage = task.exception?.message ?: "Error desconocido al registrar"
                            _authState.value = AuthState.Error(errorMessage)
                        }
                    }
            } catch (e: Exception) {
                // Captura cualquier excepción inesperada
                _authState.value = AuthState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    /**
     * Función para iniciar sesión con un correo electrónico y contraseña.
     */
    fun loginUser(email: String, password: String) {
        // Validación básica
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Correo y contraseña no pueden estar vacíos.")
            return
        }

        _authState.value = AuthState.Loading // Cambia el estado a cargando

        viewModelScope.launch { // Ejecuta la operación en un ámbito de corrutina del ViewModel
            try {
                // Realiza la llamada a Firebase para iniciar sesión
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            _authState.value = AuthState.Success // Si es exitoso, actualiza el estado a Success
                        } else {
                            // Si falla, actualiza el estado a Error con el mensaje
                            val errorMessage = task.exception?.message ?: "Credenciales inválidas o error desconocido"
                            _authState.value = AuthState.Error(errorMessage)
                        }
                    }
            } catch (e: Exception) {
                // Captura cualquier excepción inesperada
                _authState.value = AuthState.Error("Error inesperado al iniciar sesión: ${e.message}")
            }
        }
    }

    // Método para resetear el estado de autenticación. Muy importante para evitar
    // que LaunchedEffect se dispare múltiples veces al recomponerse la vista
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}