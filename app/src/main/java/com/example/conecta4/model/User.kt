// com.example.conecta4.model/User.kt
package com.example.conecta4.model

data class User(
    val uid: String = "",
    val username: String = "",
    val status: String = "offline", // Puede ser "online", "offline", "inGame"
    val inGame: String = "none", // ID del juego si está en partida, o "none"
    val lastOnline: Long = 0L // Marca de tiempo de la última conexión (para ordenar o mostrar "hace X minutos")
)