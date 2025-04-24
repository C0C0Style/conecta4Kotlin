package com.example.conecta4

import androidx.compose.ui.platform.LocalConfiguration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

// Actividad principal que maneja la interfaz de usuario
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Conecta4Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaPrincipal() // Pantalla principal del juego
                }
            }
        }
    }
}

// Pantalla principal que maneja el estado del juego
@Composable
fun PantallaPrincipal() {
    // Inicialización del tablero con 6 filas y 7 columnas (tablero de Conecta 4)
    val tablero = remember { List(6) { mutableStateListOf(*Array(7) { 0 }) } }
    // Lista que guarda las animaciones de las fichas que caen en el tablero
    val posicionesAnimadas = remember { List(6) { MutableList(7) { Animatable(-300f) } } }

    // Estado que gestiona el turno del jugador y otros estados del juego
    var turnoJugador by remember { mutableStateOf(1) }
    var mensaje by remember { mutableStateOf("Tu turno 🔴") }
    var juegoFinalizado by remember { mutableStateOf(false) }
    var celdasGanadoras by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var puedeJugar by remember { mutableStateOf(true) }

    // Scope de corutinas para manejar animaciones y lógica asíncrona
    val scope = rememberCoroutineScope()

    // Función para reiniciar el juego
    fun reiniciarJuego() {
        for (fila in 0..5) {
            for (col in 0..6) {
                tablero[fila][col] = 0 // Limpiar el tablero
                posicionesAnimadas[fila][col] = Animatable(-300f) // Reiniciar las animaciones
            }
        }
        turnoJugador = 1
        mensaje = "Tu turno 🔴"
        juegoFinalizado = false
        celdasGanadoras = emptyList()
        puedeJugar = true
    }

    // Función que maneja el turno de un jugador
    fun jugarTurno(col: Int, jugador: Int) {
        puedeJugar = false
        val filaDisponible = buscarFilaDisponible(tablero, col) // Busca la fila donde se puede colocar la ficha
        if (filaDisponible != -1) { // Si hay una fila disponible
            scope.launch {
                tablero[filaDisponible][col] = jugador // Coloca la ficha en el tablero
                // Animación de la ficha que cae
                posicionesAnimadas[filaDisponible][col].snapTo(-300f)
                posicionesAnimadas[filaDisponible][col].animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
                )

                // Verificar si hay un ganador después de colocar la ficha
                val ganadoras = verificarGanador(tablero, jugador)
                if (ganadoras.isNotEmpty()) { // Si hay un ganador
                    celdasGanadoras = ganadoras
                    val emoji = if (jugador == 1) "🔴" else "🟡"
                    mensaje = "🎉 ¡${if (jugador == 1) "Tú" else "La máquina"} gana! $emoji"
                    juegoFinalizado = true
                } else if (tablero.all { fila -> fila.all { it != 0 } }) { // Si el tablero está lleno (empate)
                    mensaje = "Empate 🤝"
                    juegoFinalizado = true
                } else { // Si el juego sigue
                    turnoJugador = if (jugador == 1) 2 else 1
                    mensaje = if (turnoJugador == 1) "Tu turno 🔴" else "Turno de la máquina 🟡"

                    // Si es el turno de la máquina, realiza su jugada automáticamente
                    if (turnoJugador == 2 && !juegoFinalizado) {
                        delay(800)
                        val colMaquina = elegirJugadaMaquina(tablero) // Elige la columna para la máquina
                        jugarTurno(colMaquina, 2) // La máquina realiza su jugada
                    } else if (turnoJugador == 1 && !juegoFinalizado) {
                        puedeJugar = true
                    }
                }
            }
        } else {
            puedeJugar = true
        }
    }

    // Diseño de la interfaz con el tablero y los controles del juego
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

        // Mostrar el tablero y permitir la interacción
        Tablero(
            tablero,
            posicionesAnimadas,
            celdasGanadoras
        ) { col ->
            if (!juegoFinalizado && turnoJugador == 1 && puedeJugar) {
                jugarTurno(col, 1)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Mostrar botón para reiniciar el juego si está finalizado
        if (juegoFinalizado) {
            Button(onClick = { reiniciarJuego() }) {
                Text("Reiniciar Juego")
            }
        }
    }
}

// Función que busca la primera fila disponible en una columna
fun buscarFilaDisponible(tablero: List<MutableList<Int>>, col: Int): Int {
    for (fila in tablero.indices.reversed()) {
        if (tablero[fila][col] == 0) return fila // Devuelve la primera fila libre
    }
    return -1 // Si no hay fila disponible, devuelve -1
}

// Función que elige la jugada de la máquina
fun elegirJugadaMaquina(tablero: List<MutableList<Int>>): Int {
    val columnasDisponibles = (0..6).filter { col -> buscarFilaDisponible(tablero, col) != -1 }
    return columnasDisponibles.random() // Elige una columna al azar de las disponibles
}

// Componente que representa el tablero de juego
@Composable
fun Tablero(
    tablero: List<MutableList<Int>>,
    posicionesAnimadas: List<List<Animatable<Float, AnimationVector1D>>>,
    celdasGanadoras: List<Pair<Int, Int>>,
    onColClick: (Int) -> Unit
) {
    val outerPad = 16.dp          // margen externo más amplio
    val innerPad = 6.dp           // espacio entre celdas/filas

    // Sección del tablero, que contiene las filas y columnas de celdas
    Column(
        modifier = Modifier
            .padding(horizontal = outerPad, vertical = outerPad / 2)
            .background(Color(0xFF1976D2))
            .shadow(4.dp)
            .padding(outerPad)
    ) {
        tablero.forEachIndexed { filaIdx, fila ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = innerPad),
                horizontalArrangement = Arrangement.spacedBy(innerPad)
            ) {
                fila.forEachIndexed { colIdx, celda ->
                    Celda(
                        ficha = celda,
                        offsetY = posicionesAnimadas[filaIdx][colIdx].value.dp,
                        parpadea = celdasGanadoras.contains(filaIdx to colIdx),
                        onClick = { onColClick(colIdx) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}

// Componente que representa una celda del tablero
@Composable
fun Celda(
    ficha: Int,
    offsetY: Dp,
    parpadea: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fichaColor = when (ficha) {
        1 -> Color.Red // Ficha del jugador 1 (roja)
        2 -> Color.Yellow // Ficha del jugador 2 (amarilla)
        else -> Color.Transparent // Sin ficha (vacío)
    }

    val bordeColor = Color(0xFF0D47A1) // Color del borde de las celdas
    val alphaAnim = rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Diseño visual de la celda
    Box(
        modifier = modifier
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bordeColor, CircleShape)
                .padding(6.dp)
        )
        if (ficha != 0) {
            Box(
                modifier = Modifier
                    .offset(y = offsetY)
                    .fillMaxSize()
                    .background(
                        fichaColor.copy(alpha = if (parpadea) alphaAnim.value else 1f),
                        CircleShape
                    )
                    .padding(8.dp)
            )
        }
    }
}

// Función que verifica si hay un ganador en el tablero
fun verificarGanador(tablero: List<List<Int>>, jugador: Int): List<Pair<Int, Int>> {
    val filas = tablero.size
    val columnas = tablero[0].size

    // Verificación horizontal
    for (fila in 0 until filas) {
        for (col in 0 until columnas - 3) {
            if ((0..3).all { offset -> tablero[fila][col + offset] == jugador }) {
                return (0..3).map { offset -> fila to (col + offset) }
            }
        }
    }

    // Verificación vertical
    for (col in 0 until columnas) {
        for (fila in 0 until filas - 3) {
            if ((0..3).all { offset -> tablero[fila + offset][col] == jugador }) {
                return (0..3).map { offset -> (fila + offset) to col }
            }
        }
    }

    // Verificación diagonal ↘
    for (fila in 0 until filas - 3) {
        for (col in 0 until columnas - 3) {
            if ((0..3).all { offset -> tablero[fila + offset][col + offset] == jugador }) {
                return (0..3).map { offset -> (fila + offset) to (col + offset) }
            }
        }
    }

    // Verificación diagonal ↙
    for (fila in 3 until filas) {
        for (col in 0 until columnas - 3) {
            if ((0..3).all { offset -> tablero[fila - offset][col + offset] == jugador }) {
                return (0..3).map { offset -> (fila - offset) to (col + offset) }
            }
        }
    }

    return emptyList() // Si no hay ganador, devuelve lista vacía
}
