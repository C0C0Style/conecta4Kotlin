package com.example.conecta4.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.conecta4.util.navRutas
import com.example.conecta4.viewModel.UsuarioViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Puedes mantener HomeViewModel para el email o mover esa lógica a UsuarioViewModel
class HomeViewModel(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) : ViewModel() {
    private val _currentUserEmail = MutableStateFlow<String?>(auth.currentUser?.email)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewHome(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(),
    usuarioViewModel: UsuarioViewModel = viewModel() // Asegúrate de que este ViewModel esté disponible
) {
    val currentUserEmail by homeViewModel.currentUserEmail.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Bienvenido") })
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = "Hola, ${currentUserEmail?.substringBefore('@') ?: "Usuario"}",
                    style = MaterialTheme.typography.headlineSmall
                )

                // --- Nuevos botones para Salas ---
                Button(onClick = {
                    navController.navigate(navRutas.createRoom)
                }) {
                    Text("Crear Sala")
                }

                Button(onClick = {
                    navController.navigate(navRutas.joinRoom)
                }) {
                    Text("Unirse a Sala")
                }
                // --- Fin Nuevos botones ---

                Button(onClick = {
                    navController.navigate(navRutas.jueMaq)
                }) {
                    Text("Jugar contra la Máquina")
                }

                Button(onClick = {
                    usuarioViewModel.logoutUser() // Llama a la función de cerrar sesión
                    navController.navigate(navRutas.iniSes) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }) {
                    Text("Cerrar Sesión")
                }
            }
        }
    )
}