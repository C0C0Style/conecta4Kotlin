package com.example.conecta4.view

import android.widget.Toast // Importar Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Importar LocalContext para Toast
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Importar viewModel()
import com.example.conecta4.viewModel.AuthState // Importar AuthState
import com.example.conecta4.viewModel.UsuarioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewInicioSesion(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    usuarioViewModel: UsuarioViewModel
) {
    var email by remember { mutableStateOf("") } // Cambiado a 'email' para consistencia con Firebase
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current // Obtener el contexto para los Toast

    // Recolectar el estado de autenticación del ViewModel
    val authState by usuarioViewModel.authState.collectAsState()

    // Observar los cambios en el estado de autenticación
    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Success -> {
                Toast.makeText(context, "Inicio de sesión exitoso. ¡Bienvenido!", Toast.LENGTH_LONG).show()
                onLoginSuccess() // Llamar a la función de navegación de éxito
                usuarioViewModel.resetAuthState() // Resetear el estado para futuras operaciones
            }
            is AuthState.Error -> {
                val errorMessage = (authState as AuthState.Error).message
                Toast.makeText(context, "Error al iniciar sesión: $errorMessage", Toast.LENGTH_LONG).show()
                usuarioViewModel.resetAuthState() // Resetear el estado para permitir otro intento
            }
            AuthState.Loading -> {
                // Opcional: Mostrar un Toast o Snackbar de "Iniciando sesión..."
            }
            AuthState.Idle -> {
                // Estado inicial o reseteado, no hacer nada.
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Iniciar Sesión",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email, // Usar 'email' en lugar de 'username'
            onValueChange = { email = it },
            label = { Text("Correo Electrónico") }, // Cambiar etiqueta
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Icono de Correo") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Icono de Contraseña") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        ElevatedButton(
            onClick = {
                // Llamar a la función de inicio de sesión del ViewModel
                usuarioViewModel.loginUser(email, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            // Deshabilitar el botón mientras la operación está en curso
            enabled = authState != AuthState.Loading
        ) {
            Text(if (authState is AuthState.Loading) "Iniciando..." else "Iniciar Sesión")
        }

        // Opcional: Mostrar un indicador de progreso
        if (authState is AuthState.Loading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                onNavigateToRegister() // Llama a la función de navegación para ir al registro
            }
        ) {
            Text("¿No tienes cuenta? Regístrate aquí")
        }
    }
}
