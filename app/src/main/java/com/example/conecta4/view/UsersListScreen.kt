package com.example.conecta4.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Importa la función viewModel()
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

import com.example.conecta4.model.User // Asegúrate de importar tu modelo User
import com.example.conecta4.viewModel.UsersListViewModel // ¡IMPORTANTE! Importa tu UsersListViewModel desde su ubicación correcta
import com.example.conecta4.viewModel.UsuarioViewModel // Si también necesitas el UsuarioViewModel aquí

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class) // Anotación para TopAppBar
@Composable
fun UsersListScreen(
    navController: NavController,
    // Aquí es donde obtienes la instancia de tu ViewModel.
    // NO defines la clase UsersListViewModel aquí.
    usersListViewModel: UsersListViewModel = viewModel(),
    // Si necesitas UsuarioViewModel para interactuar con el usuario actual o el juego, inyecta aquí también:
    usuarioViewModel: UsuarioViewModel = viewModel()
) {
    // Observa el LiveData 'users' del UsersListViewModel
    val users by usersListViewModel.users.observeAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Usuarios Conectados") })
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Aplica el padding del Scaffold
            ) {
                if (users.isEmpty()) {
                    Text("No hay otros usuarios conectados en este momento.", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(users) { user ->
                            UserListItem(user = user)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun UserListItem(user: User) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Usuario: ${user.username}",
                style = MaterialTheme.typography.titleMedium
            )
            val statusColor = when (user.status) {
                "online" -> Color.Green
                "offline" -> Color.Gray
                else -> Color.LightGray
            }
            Text(
                text = "Estado: ${user.status.capitalize()}", // Capitalize para mejor presentación
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )
            if (user.inGame != "false" && user.inGame.isNotBlank()) {
                Text(
                    text = "En Juego: ${user.inGame}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Blue
                )
            } else {
                Text(
                    text = "No en Juego",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            // Opcional: Mostrar lastOnline formateado
            // val formattedTime = java.text.SimpleDateFormat("HH:mm:ss dd/MM", java.util.Locale.getDefault()).format(java.util.Date(user.lastOnline))
            // Text(text = "Última conexión: $formattedTime", style = MaterialTheme.typography.bodySmall)
        }
    }
}
/*
@Preview(showBackground = true, widthDp = 320)
@Composable
fun PreviewUsersListScreen() {
    UsersListScreen(navController = rememberNavController())
}
 */