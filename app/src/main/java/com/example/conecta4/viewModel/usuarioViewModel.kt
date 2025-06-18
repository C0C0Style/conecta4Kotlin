package com.example.conecta4.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject // Si estás usando Dagger Hilt o similar, mantenlo. Si no, puedes quitarlo.

// Define los posibles estados de la operación de autenticación
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

// Puedes añadir una interfaz si prefieres desacoplar el ViewModel de la implementación de Firebase
// interface IUsuarioViewModel {
//     fun registerUser(email: String, password: String)
//     fun loginUser(email: String, password: String)
//     fun setStatusOfflineAndLogout()
//     fun setPlayerInGame(gameId: String)
//     fun setPlayerNotInGame()
//     fun resetAuthState()
// }

class UsuarioViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) : ViewModel() { // Puedes implementar IUsuarioViewModel aquí si lo creaste

    private val usersRef: DatabaseReference = database.getReference("users")
    private val usernamesRef: DatabaseReference = database.getReference("usernames")

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    // Variable para almacenar el UID del usuario actual
    // Se inicializa a null y se actualiza al iniciar sesión o registrarse.
    private var currentUserId: String? = null

    // Listener para el estado de conexión a Firebase
    private var connectedRef: DatabaseReference? = null
    private var connectedListener: ValueEventListener? = null

    // --- Métodos de ciclo de vida del ViewModel ---
    init {
        // Observar el estado de autenticación de Firebase
        // Esto es útil para manejar reconexiones o si el usuario ya está logueado al iniciar la app
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null && currentUserId == null) {
                // Usuario recién logueado o app iniciada con sesión activa
                currentUserId = user.uid
                setupUserPresence() // Configurar la presencia
                monitorConnectionState() // Iniciar monitoreo de conexión
            } else if (user == null && currentUserId != null) {
                // Usuario se deslogueó (o token expiró, etc.)
                currentUserId = null
                stopMonitoringConnectionState() // Detener monitoreo
                // setStatusOfflineAndLogout() ya debería manejar la limpieza si fue un logout manual
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Cuando el ViewModel se destruye (ej. la actividad se cierra permanentemente)
        // Intentamos poner el usuario offline, aunque onDisconnect() es el más fiable
        // para cierres abruptos.
        // Solo llamamos a setStatusOffline() si queremos una limpieza explícita aquí,
        // pero onDisconnect() ya debería estar activo.
        // setStatusOffline() // Descomentar solo si onDisconnect no es suficiente por algún motivo específico.

        // Detener el monitoreo de conexión para evitar fugas de memoria
        stopMonitoringConnectionState()
        Log.d("UsuarioViewModel", "UsuarioViewModel onCleared. Listeners limpiados.")
    }

    // --- Lógica de Autenticación y Registro ---

    fun registerUser(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Correo y contraseña no pueden estar vacíos.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("La contraseña debe tener al menos 6 caracteres.")
            return
        }

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            val user = authTask.result?.user
                            if (user != null) {
                                currentUserId = user.uid // Almacenar el UID del usuario recién registrado
                                val userEmail = user.email

                                val baseUsername = if (userEmail != null) {
                                    userEmail.substringBefore("@")
                                        .replace(".", "")
                                        .replace("_", "")
                                        .replace("-", "")
                                        .lowercase()
                                } else {
                                    "player_${user.uid.substring(0, 6).lowercase()}"
                                }

                                findUniqueUsernameAndSetProfile(user.uid, baseUsername, 0)

                            } else {
                                _authState.value = AuthState.Error("Error al obtener usuario autenticado.")
                                Log.e("UsuarioViewModel", "Auth successful but user object is null.")
                            }
                        } else {
                            val errorMessage = authTask.exception?.message ?: "Error desconocido al registrar"
                            _authState.value = AuthState.Error(errorMessage)
                            Log.e("UsuarioViewModel", "Auth registration failed: $errorMessage")
                        }
                    }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error inesperado: ${e.message}")
                Log.e("UsuarioViewModel", "Unexpected error during registration: ${e.message}")
            }
        }
    }

    /**
     * Función recursiva para encontrar un nombre de usuario único y establecer el perfil del usuario
     * en Realtime Database.
     */
    private fun findUniqueUsernameAndSetProfile(uid: String, baseName: String, attempt: Int) {
        val finalUsername = if (attempt == 0) baseName else "$baseName$attempt"
        val usernameLowercase = finalUsername.lowercase()

        usernamesRef.child(usernameLowercase).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    usernamesRef.child(usernameLowercase).setValue(uid)
                        .addOnSuccessListener {
                            val initialUserData = mapOf(
                                "uid" to uid,
                                "username" to finalUsername,
                                "status" to "offline", // Estado inicial
                                "inGame" to "false",  // Usamos "false" para indicar que no está en juego
                                "lastOnline" to ServerValue.TIMESTAMP // Marca de tiempo
                            )
                            usersRef.child(uid).setValue(initialUserData)
                                .addOnSuccessListener {
                                    // ¡Éxito completo!
                                    _authState.value = AuthState.Success
                                    Log.d("UsuarioViewModel", "Perfil de usuario y nombre único '$finalUsername' creados para $uid.")
                                    setupUserPresence() // Configurar la presencia después de crear el perfil
                                    monitorConnectionState() // Iniciar monitoreo de conexión
                                }
                                .addOnFailureListener { e ->
                                    // Si falla la creación del perfil, revertir el registro del username
                                    usernamesRef.child(usernameLowercase).removeValue()
                                    _authState.value = AuthState.Error("Error al guardar perfil de usuario: ${e.message}")
                                    Log.e("UsuarioViewModel", "Fallo al crear perfil de usuario en DB: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            _authState.value = AuthState.Error("Error al registrar nombre de usuario: ${e.message}")
                            Log.e("UsuarioViewModel", "Fallo al registrar nombre de usuario en índice: ${e.message}")
                        }
                } else {
                    Log.d("UsuarioViewModel", "Nombre de usuario '$finalUsername' ya tomado, intentando con '${baseName}${attempt + 1}'.")
                    findUniqueUsernameAndSetProfile(uid, baseName, attempt + 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _authState.value = AuthState.Error("Error de conexión al verificar nombre de usuario: ${error.message}")
                Log.e("UsuarioViewModel", "Error de DB al verificar unicidad del nombre de usuario: ${error.message}")
            }
        })
    }

    /**
     * Función para iniciar sesión con un correo electrónico y contraseña.
     */
    fun loginUser(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Correo y contraseña no pueden estar vacíos.")
            return
        }

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            currentUserId = auth.currentUser?.uid // Almacenar el UID del usuario logueado
                            setupUserPresence() // Configurar la presencia después del login exitoso
                            monitorConnectionState() // Iniciar monitoreo de conexión
                            _authState.value = AuthState.Success
                        } else {
                            val errorMessage = task.exception?.message ?: "Credenciales inválidas o error desconocido"
                            _authState.value = AuthState.Error(errorMessage)
                            Log.e("UsuarioViewModel", "Auth login failed: $errorMessage")
                        }
                    }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error inesperado al iniciar sesión: ${e.message}")
                Log.e("UsuarioViewModel", "Unexpected error during login: ${e.message}")
            }
        }
    }

    // --- Lógica de Presencia y Estado ---

    /**
     * Configura los listeners de presencia de Firebase para el usuario actual.
     * Esto incluye establecer el estado online y configurar onDisconnect().
     */
    private fun setupUserPresence() {
        val uid = currentUserId ?: return // Si no hay UID, no hacemos nada
        val userRef = usersRef.child(uid)

        // 1. Establecer el estado a "online" y actualizar lastOnline
        userRef.child("status").setValue("online")
        userRef.child("lastOnline").setValue(ServerValue.TIMESTAMP)
        userRef.child("inGame").setValue("false") // Asegurarse de que no esté en juego al iniciar

        // 2. Usar onDisconnect() para manejar la desconexión inesperada
        // Esto se ejecutará cuando el cliente se desconecte del servidor de Firebase
        userRef.child("status").onDisconnect().setValue("offline")
        userRef.child("lastOnline").onDisconnect().setValue(ServerValue.TIMESTAMP)
        userRef.child("inGame").onDisconnect().setValue("false")
        Log.d("UsuarioViewModel", "onDisconnect listeners configurados para usuario $uid.")
    }

    /**
     * Establece el estado del usuario a "offline" y cancela las operaciones onDisconnect().
     * Debe llamarse antes de cerrar sesión.
     */
    fun setStatusOfflineAndLogout() {
        val uid = currentUserId ?: run {
            // Si no hay UID, el usuario ya no está logueado o hubo un error, simplemente cerramos sesión.
            auth.signOut()
            currentUserId = null // Limpiar el UID almacenado
            _authState.value = AuthState.Idle // Reiniciar estado
            stopMonitoringConnectionState() // Detener monitoreo
            return
        }

        val userRef = usersRef.child(uid)

        // 1. Cancelar las operaciones onDisconnect para evitar que se disparen después del logout manual
        userRef.child("status").onDisconnect().cancel()
        userRef.child("lastOnline").onDisconnect().cancel()
        userRef.child("inGame").onDisconnect().cancel()
        Log.d("UsuarioViewModel", "onDisconnect listeners cancelados para usuario $uid.")

        // 2. Establecer el estado a "offline" y "inGame" a "false" explícitamente
        val updates = hashMapOf<String, Any>(
            "status" to "offline",
            "inGame" to "false",
            "lastOnline" to ServerValue.TIMESTAMP
        )
        userRef.updateChildren(updates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("UsuarioViewModel", "Estado offline establecido y actualizado lastOnline para $uid.")
                } else {
                    Log.e("UsuarioViewModel", "Error al establecer estado offline para $uid: ${task.exception?.message}")
                }
                // En cualquier caso, proceder a cerrar la sesión de Firebase Auth
                auth.signOut()
                currentUserId = null // Limpiar el UID almacenado
                _authState.value = AuthState.Idle // Resetear el estado de autenticación
                stopMonitoringConnectionState() // Detener monitoreo
            }
    }

    /**
     * Establece el estado 'inGame' del usuario a un gameId específico.
     * @param gameId El ID del juego al que el usuario ha entrado.
     */
    fun setPlayerInGame(gameId: String) {
        val uid = currentUserId ?: return
        usersRef.child(uid).child("inGame").setValue(gameId)
            .addOnSuccessListener {
                Log.d("UsuarioViewModel", "Usuario $uid ahora en juego: $gameId")
            }
            .addOnFailureListener { e ->
                Log.e("UsuarioViewModel", "Error al establecer inGame para $uid: ${e.message}")
            }
    }

    /**
     * Establece el estado 'inGame' del usuario a "false" (no en juego).
     */
    fun setPlayerNotInGame() {
        val uid = currentUserId ?: return
        usersRef.child(uid).child("inGame").setValue("false") // "false" es el valor por defecto en tu modelo
            .addOnSuccessListener {
                Log.d("UsuarioViewModel", "Usuario $uid ahora no está en juego.")
            }
            .addOnFailureListener { e ->
                Log.e("UsuarioViewModel", "Error al establecer no en juego para $uid: ${e.message}")
            }
    }

    // --- Monitoreo del estado de conexión a Firebase ---
    private fun monitorConnectionState() {
        if (connectedRef == null) { // Asegurarse de adjuntar el listener solo una vez
            connectedRef = database.getReference(".info/connected")
            connectedListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        Log.d("ConnectionMonitor", "Cliente CONECTADO a Firebase Realtime Database.")
                        // Opcional: Reafirmar el estado online si se perdió la conexión y se recuperó.
                        // setupUserPresence() // Podrías llamarlo aquí, pero init y login/register ya lo hacen.
                    } else {
                        Log.d("ConnectionMonitor", "Cliente DESCONECTADO de Firebase Realtime Database. onDisconnect() debería activarse.")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ConnectionMonitor", "Listener de conexión cancelado: ${error.message}")
                }
            }
            connectedRef?.addValueEventListener(connectedListener!!)
            Log.d("ConnectionMonitor", "Iniciado monitoreo de .info/connected.")
        }
    }

    private fun stopMonitoringConnectionState() {
        connectedRef?.removeEventListener(connectedListener!!)
        connectedRef = null
        connectedListener = null
        Log.d("ConnectionMonitor", "Detenido monitoreo de .info/connected.")
    }

    // Método para resetear el estado de autenticación. Muy importante para evitar
    // que LaunchedEffect se dispare múltiples veces al recomponerse la vista
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}