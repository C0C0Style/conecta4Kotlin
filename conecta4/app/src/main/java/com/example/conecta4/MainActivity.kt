package com.example.conecta4

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
import kotlin.random.Random

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
    val scope = rememberCoroutineScope()

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
    }

    fun jugarTurno(col: Int, jugador: Int) {
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
                    }
                }
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
        Text("¡Bienvenido a Conecta 4!", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = mensaje, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(32.dp))

        Tablero(tablero, posicionesAnimadas, celdasGanadoras) { col ->
            if (!juegoFinalizado && turnoJugador == 1) {
                jugarTurno(col, 1)
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