// Archivo: com.example.conecta4.view/viewRegistro.kt

package com.example.conecta4.view

import android.widget.Toast // Importa Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.* // Importa remember, mutableStateOf, LaunchedEffect, collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Importa LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Importa viewModel() para obtener la instancia del ViewModel
import androidx.navigation.NavController
import com.example.conecta4.util.navRutas
import com.example.conecta4.viewModel.AuthState // Importa el sealed class AuthState
import com.example.conecta4.viewModel.UsuarioViewModel // Importa tu ViewModel

@OptIn(ExperimentalMaterial3Api::class) // Podrías necesitar esto dependiendo de tus componentes Material3
@Composable
fun ViewRegistro(
    navController: NavController, // El NavController es necesario para navegar
    usuarioViewModel: UsuarioViewModel = viewModel() // Obtiene la instancia de tu ViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current // Contexto para mostrar Toasts

    // Colecta el estado del flujo de autenticación del ViewModel.
    // Esto hace que el Composable se recomponga cada vez que el estado cambia.
    val authState by usuarioViewModel.authState.collectAsState()

    // LaunchedEffect se ejecuta cuando las claves cambian (en este caso, authState).
    // Es el lugar ideal para efectos secundarios como la navegación o mostrar Toasts.
    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Success -> {
                // SOLO SI EL REGISTRO FUE EXITOSO:
                Toast.makeText(context, "Registro exitoso. ¡Inicia sesión ahora!", Toast.LENGTH_LONG).show()

                // NAVEGACIÓN A LA PANTALLA DE INICIO DE SESIÓN
                navController.navigate(navRutas.iniSes) {
                    // Esto es crucial: elimina la pantalla de registro de la pila de navegación.
                    // El usuario no podrá volver al formulario de registro presionando "atrás".
                    popUpTo(navRutas.registro) { inclusive = true }
                }

                // Importante: Resetea el estado del ViewModel después de la navegación.
                // Esto previene que si el Composable se recompone por alguna razón,
                // no intente navegar de nuevo con el mismo estado "Success".
                usuarioViewModel.resetAuthState()
            }
            is AuthState.Error -> {
                // Si hay un error en el registro, muestra un Toast
                val errorMessage = (authState as AuthState.Error).message
                Toast.makeText(context, "Error al registrar: $errorMessage", Toast.LENGTH_LONG).show()
                // Resetea el estado para permitir que el usuario intente de nuevo
                usuarioViewModel.resetAuthState()
            }
            AuthState.Loading -> {
                // Opcional: Mostrar un mensaje de "Cargando..." o un ProgressBar
            }
            AuthState.Idle -> {
                // Estado inicial o reseteado, no hacer nada aquí.
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Regístrate", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo Electrónico") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                usuarioViewModel.registerUser(email, password)
            },
            modifier = Modifier.fillMaxWidth(),
            // Deshabilita el botón mientras el registro está en progreso
            enabled = authState != AuthState.Loading
        ) {
            Text(if (authState is AuthState.Loading) "Registrando..." else "Registrarse")
        }

        // Muestra un indicador de progreso si la operación está en curso
        if (authState is AuthState.Loading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = {
            // Permite al usuario ir a la pantalla de inicio de sesión si ya tiene cuenta
            navController.navigate(navRutas.iniSes) {
                popUpTo(navRutas.registro) { inclusive = true }
            }
        }) {
            Text("¿Ya tienes una cuenta? Inicia sesión")
        }
    }
}