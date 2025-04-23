package com.example.conecta4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.conecta4.ui.theme.Conecta4Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Conecta4Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaPrincipal()
                }
            }
        }
    }
}

@Composable
fun PantallaPrincipal() {
    val tablero = remember {
        List(6) { mutableStateListOf(*Array(7) { 0 }) }
    }
    val posicionesAnimadas = remember {
        List(6) { MutableList(7) { Animatable(-300f) } }
    }

    var turnoJugador by remember { mutableStateOf(1) }
    var mensaje by remember { mutableStateOf("Tu turno 🔴") }
    var juegoFinalizado by remember { mutableStateOf(false) }
    var celdasGanadoras by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var puedeJugar by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    val scrollH = rememberScrollState()

    fun reiniciarJuego() {
        for (fila in 0..5) {
            for (col in 0..6) {
                tablero[fila][col] = 0
                posicionesAnimadas[fila][col] = Animatable(-300f)
            }
        }
        turnoJugador = 1
        mensaje = "Tu turno 🔴"
        juegoFinalizado = false
        celdasGanadoras = emptyList()
        puedeJugar = true
    }

    fun jugarTurno(col: Int, jugador: Int) {
        puedeJugar = false
        val filaDisponible = buscarFilaDisponible(tablero, col)
        if (filaDisponible != -1) {
            scope.launch {
                tablero[filaDisponible][col] = jugador
                posicionesAnimadas[filaDisponible][col].snapTo(-300f)
                posicionesAnimadas[filaDisponible][col].animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
                )

                val ganadoras = verificarGanador(tablero, jugador)
                if (ganadoras.isNotEmpty()) {
                    celdasGanadoras = ganadoras
                    val emoji = if (jugador == 1) "🔴" else "🟡"
                    mensaje = "🎉 ¡${if (jugador == 1) "Tú" else "La máquina"} gana! $emoji"
                    juegoFinalizado = true
                } else if (tablero.all { fila -> fila.all { it != 0 } }) {
                    mensaje = "Empate 🤝"
                    juegoFinalizado = true
                } else {
                    turnoJugador = if (jugador == 1) 2 else 1
                    mensaje = if (turnoJugador == 1) "Tu turno 🔴" else "Turno de la máquina 🟡"

                    if (turnoJugador == 2 && !juegoFinalizado) {
                        delay(800)
                        val colMaquina = elegirJugadaMaquina(tablero)
                        jugarTurno(colMaquina, 2)
                    } else if (turnoJugador == 1 && !juegoFinalizado) {
                        puedeJugar = true
                    }
                }
            }
        } else {
            puedeJugar = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¡Bienvenido a Conecta 4!", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = mensaje, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(32.dp))

        Box(modifier = Modifier.horizontalScroll(scrollH)) {
            Tablero(tablero, posicionesAnimadas, celdasGanadoras) { col ->
                if (!juegoFinalizado && turnoJugador == 1 && puedeJugar) {
                    jugarTurno(col, 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (juegoFinalizado) {
            Button(onClick = { reiniciarJuego() }) {
                Text("Reiniciar Juego")
            }
        }
    }
}

fun buscarFilaDisponible(tablero: List<MutableList<Int>>, col: Int): Int {
    for (fila in tablero.indices.reversed()) {
        if (tablero[fila][col] == 0) return fila
    }
    return -1
}

fun elegirJugadaMaquina(tablero: List<MutableList<Int>>): Int {
    val columnasDisponibles = (0..6).filter { col -> buscarFilaDisponible(tablero, col) != -1 }
    return columnasDisponibles.random()
}

@Composable
fun Tablero(
    tablero: List<MutableList<Int>>,
    posicionesAnimadas: List<List<Animatable<Float, AnimationVector1D>>>,
    celdasGanadoras: List<Pair<Int, Int>>,
    onColClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .background(Color(0xFF1976D2))
            .padding(8.dp)
            .shadow(4.dp)
    ) {
        for ((filaIndex, fila) in tablero.withIndex()) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                for ((colIndex, celda) in fila.withIndex()) {
                    Celda(
                        ficha = celda,
                        offsetY = posicionesAnimadas[filaIndex][colIndex].value.dp,
                        parpadea = celdasGanadoras.contains(filaIndex to colIndex),
                        onClick = { onColClick(colIndex) }
                    )
                }
            }
        }
    }
}

@Composable
fun Celda(ficha: Int, offsetY: Dp, parpadea: Boolean, onClick: () -> Unit) {
    val fichaColor = when (ficha) {
        1 -> Color.Red
        2 -> Color.Yellow
        else -> Color.Transparent
    }

    val bordeColor = Color(0xFF0D47A1)
    val alphaAnim = rememberInfiniteTransition(label = "parpadeo").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(60.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(bordeColor, shape = CircleShape)
        )

        if (ficha != 0) {
            Box(
                modifier = Modifier
                    .offset(y = offsetY)
                    .size(40.dp)
                    .background(
                        color = fichaColor.copy(alpha = if (parpadea) alphaAnim.value else 1f),
                        shape = CircleShape
                    )
            )
        }
    }
}

fun verificarGanador(tablero: List<List<Int>>, jugador: Int): List<Pair<Int, Int>> {
    val filas = tablero.size
    val columnas = tablero[0].size

    // Horizontal
    for (fila in 0 until filas) {
        for (col in 0 until columnas - 3) {
            if ((0..3).all { offset -> tablero[fila][col + offset] == jugador }) {
                return (0..3).map { offset -> fila to (col + offset) }
            }
        }
    }

    // Vertical
    for (col in 0 until columnas) {
        for (fila in 0 until filas - 3) {
            if ((0..3).all { offset -> tablero[fila + offset][col] == jugador }) {
                return (0..3).map { offset -> (fila + offset) to col }
            }
        }
    }

    // Diagonal ↘
    for (fila in 0 until filas - 3) {
        for (col in 0 until columnas - 3) {
            if ((0..3).all { offset -> tablero[fila + offset][col + offset] == jugador }) {
                return (0..3).map { offset -> (fila + offset) to (col + offset) }
            }
        }
    }

    // Diagonal ↙
    for (fila in 3 until filas) {
        for (col in 0 until columnas - 3) {
            if ((0..3).all { offset -> tablero[fila - offset][col + offset] == jugador }) {
                return (0..3).map { offset -> (fila - offset) to (col + offset) }
            }
        }
    }

    return emptyList()
}
