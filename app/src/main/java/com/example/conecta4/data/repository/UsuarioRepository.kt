package com.example.conecta4.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await // Para usar .await() en las tareas de Firebase
import android.util.Log // Para mensajes de depuración

class UsuarioRepository { // Sin @Inject ni parámetros en el constructor si no se usa Dagger Hilt

    private val auth: FirebaseAuth = FirebaseAuth.getInstance() // Obtenemos la instancia de Firebase Auth

    /**
     * Registra un nuevo usuario en Firebase Authentication.
     * @throws IllegalArgumentException si los datos son inválidos.
     * @throws Exception si Firebase falla al registrar el usuario.
     */
    suspend fun registerUser(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("El correo y la contraseña no pueden estar vacíos.")
        }
        if (password.length < 6) {
            throw IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.")
        }

        try {
            // Intenta crear el usuario. .await() lanzará una excepción si falla.
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            if (authResult.user != null) {
                Log.d("UsuarioRepository", "Usuario registrado con éxito: ${authResult.user?.email}")
            } else {
                // Esto es una precaución, ya que .await() debería manejar la mayoría de los errores.
                throw IllegalStateException("Error desconocido: el objeto de usuario es nulo después del registro.")
            }
        } catch (e: Exception) {
            Log.e("UsuarioRepository", "Fallo al registrar usuario: ${e.message}")
            throw e // Re-lanza la excepción para que el ViewModel la maneje.
        }
    }

    /**
     * Inicia sesión con un usuario existente en Firebase Authentication.
     * @throws IllegalArgumentException si los datos son inválidos.
     * @throws Exception si Firebase falla al iniciar sesión.
     */
    suspend fun loginUser(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("El correo y la contraseña no pueden estar vacíos.")
        }

        try {
            // Intenta iniciar sesión. .await() lanzará una excepción si falla.
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            if (authResult.user != null) {
                Log.d("UsuarioRepository", "Usuario inició sesión con éxito: ${authResult.user?.email}")
            } else {
                // Esto es una precaución.
                throw IllegalStateException("Error desconocido: el objeto de usuario es nulo después del inicio de sesión.")
            }
        } catch (e: Exception) {
            Log.e("UsuarioRepository", "Fallo al iniciar sesión: ${e.message}")
            throw e // Re-lanza la excepción para que el ViewModel la maneje.
        }
    }
    fun logoutUser() {
        try {
            auth.signOut()
            Log.d("UsuarioRepository", "Sesión de usuario cerrada con éxito.")
        } catch (e: Exception) {
            Log.e("UsuarioRepository", "Error al cerrar sesión: ${e.message}")
            // Podrías lanzar una excepción aquí si el error es crítico para la UI
            // o manejarlo de otra forma si solo es un log.
            throw e
        }
    }
}