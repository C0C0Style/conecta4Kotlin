// app/src/main/java/com/example/conecta4/view/GameScreen.kt
package com.example.conecta4.view

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.conecta4.viewmodel.GameViewModel

// Pantalla principal que maneja el estado del juego
@Composable
fun GameScreen(gameViewModel: GameViewModel = viewModel()) {
    // Observa los estados del ViewModel
    val tablero by gameViewModel.tablero.collectAsState()
    val posicionesAnimadas by gameViewModel.posicionesAnimadas.collectAsState()
    val mensajeJuego by gameViewModel.mensajeJuego.collectAsState()
    val juegoTerminado by gameViewModel.juegoTerminado.collectAsState()
    val celdasGanadoras by gameViewModel.celdasGanadoras.collectAsState()
    val jugadorActual by gameViewModel.jugadorActual.collectAsState()
    val puedeJugar by gameViewModel.puedeJugar.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¡\uD83D\uDC7E Bienvenido \uD83D\uDC7E! ", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 32.sp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = mensajeJuego, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(32.dp))

        Board(
            tablero = tablero,
            posicionesAnimadas = posicionesAnimadas,
            celdasGanadoras = celdasGanadoras
        ) { col ->
            if (!juegoTerminado && jugadorActual == 1 && puedeJugar) {
                gameViewModel.jugarTurno(col, 1)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (juegoTerminado) {
            Button(onClick = { gameViewModel.reiniciarJuego() }) {
                Text("Reiniciar Juego")
            }
        }
    }
}

// Componente que representa el tablero de juego
@Composable
fun Board(
    tablero: List<MutableList<Int>>,
    posicionesAnimadas: List<List<Animatable<Float, AnimationVector1D>>>,
    celdasGanadoras: List<Pair<Int, Int>>,
    onColClick: (Int) -> Unit
) {
    val outerPad = 16.dp
    val innerPad = 6.dp

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
                    Cell(
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

@Composable
fun Cell(
    ficha: Int,
    offsetY: Dp,
    parpadea: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fichaColor = when (ficha) {
        1 -> Color.Red
        2 -> Color.Yellow
        else -> Color.Transparent
    }

    val bordeColor = Color(0xFF0D47A1)
    val alphaAnim = rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

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