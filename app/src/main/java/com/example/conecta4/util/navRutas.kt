package com.example.conecta4.util

object navRutas {
    const val iniSes = "inicio_sesion"
    const val registro = "registro"
    const val inicio = "home"
    const val jueMaq = "jugar_maquina"
    const val usersList = "lista_usuarios" // Si lo usas para la lista de usuarios

    // --- Nuevas Rutas de Salas ---
    const val createRoom = "crear_sala"
    const val joinRoom = "unirse_sala"
    const val waitingRoom = "sala_espera" // Para cuando se crea o se une y espera a otro jugador
    const val gameRoom = "sala_juego" // Cuando el juego multiplayer realmente comienza
}