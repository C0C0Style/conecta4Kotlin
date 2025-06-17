// app/src/main/java/com/example/conecta4/viewmodel/GameViewModel.kt
package com.example.conecta4.viewmodel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    // Estado del tablero de juego: 0 para vacío, 1 para Jugador 1 (Rojo), 2 para Jugador 2 (Amarillo/IA)
    private val _tablero = MutableStateFlow(List(6) { mutableStateListOf(*Array(7) { 0 }) })
    val tablero: StateFlow<List<MutableList<Int>>> = _tablero.asStateFlow()

    // Estado de la animación para las fichas que caen
    private val _posicionesAnimadas = MutableStateFlow(List(6) { MutableList(7) { Animatable(-300f) } })
    val posicionesAnimadas: StateFlow<List<MutableList<Animatable<Float, AnimationVector1D>>>> = _posicionesAnimadas.asStateFlow()

    private val _jugadorActual = MutableStateFlow(1) // 1 para Jugador 1, 2 para IA
    val jugadorActual: StateFlow<Int> = _jugadorActual.asStateFlow()

    private val _mensajeJuego = MutableStateFlow("Tu turno 🔴")
    val mensajeJuego: StateFlow<String> = _mensajeJuego.asStateFlow()

    private val _juegoTerminado = MutableStateFlow(false)
    val juegoTerminado: StateFlow<Boolean> = _juegoTerminado.asStateFlow()

    private val _celdasGanadoras = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val celdasGanadoras: StateFlow<List<Pair<Int, Int>>> = _celdasGanadoras.asStateFlow()

    private val _puedeJugar = MutableStateFlow(true)
    val puedeJugar: StateFlow<Boolean> = _puedeJugar.asStateFlow()

    init {
        // Inicializa el juego al crear el ViewModel
        reiniciarJuego()
    }

    fun reiniciarJuego() {
        _tablero.value = List(6) { mutableStateListOf(*Array(7) { 0 }) }
        _posicionesAnimadas.value = List(6) { MutableList(7) { Animatable(-300f) } }
        _jugadorActual.value = 1
        _mensajeJuego.value = "Tu turno 🔴"
        _juegoTerminado.value = false
        _celdasGanadoras.value = emptyList()
        _puedeJugar.value = true
    }

    fun jugarTurno(col: Int, jugador: Int) {
        _puedeJugar.value = false
        viewModelScope.launch {
            val filaDisponible = buscarFilaDisponible(_tablero.value, col)
            if (filaDisponible != -1) {
                _tablero.value[filaDisponible][col] = jugador

                // Animar la ficha
                _posicionesAnimadas.value[filaDisponible][col].snapTo(-300f)
                _posicionesAnimadas.value[filaDisponible][col].animateTo(
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 500, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                )

                val ganadoras = verificarGanador(_tablero.value, jugador)
                if (ganadoras.isNotEmpty()) {
                    _celdasGanadoras.value = ganadoras
                    val emoji = if (jugador == 1) "🔴" else "🟡"
                    _mensajeJuego.value = "🎉 ¡${if (jugador == 1) "Tú ganas" else "La máquina gana"} ! $emoji"
                    _juegoTerminado.value = true
                } else if (_tablero.value.all { fila -> fila.all { it != 0 } }) {
                    _mensajeJuego.value = "Empate 🤝"
                    _juegoTerminado.value = true
                } else {
                    _jugadorActual.value = if (jugador == 1) 2 else 1
                    _mensajeJuego.value = if (_jugadorActual.value == 1) "Tu turno 🔴" else "Turno de la máquina 🟡"

                    if (_jugadorActual.value == 2 && !_juegoTerminado.value) {
                        delay(800)
                        val colMaquina = elegirJugadaMaquina(_tablero.value)
                        jugarTurno(colMaquina, 2)
                    } else if (_jugadorActual.value == 1 && !_juegoTerminado.value) {
                        _puedeJugar.value = true
                    }
                }
            } else {
                _puedeJugar.value = true
            }
        }
    }

    // --- Funciones auxiliares de la lógica del juego (Movidas desde PantallaPrincipal) ---

    private fun buscarFilaDisponible(tablero: List<MutableList<Int>>, col: Int): Int {
        for (fila in tablero.indices.reversed()) {
            if (tablero[fila][col] == 0) return fila
        }
        return -1
    }

    private fun elegirJugadaMaquina(tablero: List<MutableList<Int>>): Int {
        val columnasDisponibles = (0..6).filter { col -> buscarFilaDisponible(tablero, col) != -1 }
        return columnasDisponibles.random()
    }

    private fun verificarGanador(tablero: List<MutableList<Int>>, jugador: Int): List<Pair<Int, Int>> {
        // Chequeos horizontal, vertical y diagonal para un ganador de Conecta 4
        for (row in 0 until 6) {
            for (col in 0 until 7) {
                if (col + 3 < 7 && (0 until 4).all { i -> tablero[row][col + i] == jugador }) {
                    return (0 until 4).map { i -> row to col + i } // Victoria horizontal
                }
                if (row + 3 < 6 && (0 until 4).all { i -> tablero[row + i][col] == jugador }) {
                    return (0 until 4).map { i -> row + i to col } // Victoria vertical
                }
                if (row + 3 < 6 && col + 3 < 7 && (0 until 4).all { i -> tablero[row + i][col + i] == jugador }) {
                    return (0 until 4).map { i -> row + i to col + i } // Victoria diagonal (descendente)
                }
                if (row - 3 >= 0 && col + 3 < 7 && (0 until 4).all { i -> tablero[row - i][col + i] == jugador }) {
                    return (0 until 4).map { i -> row - i to col + i } // Victoria diagonal (ascendente)
                }
            }
        }
        return emptyList()
    }
}