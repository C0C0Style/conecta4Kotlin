package com.example.conecta4.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Celda(
    ficha: Int,
    offsetY: Dp,
    parpadea: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fichaColor = when (ficha) {
        1 -> Color.Red // Ficha del jugador 1 (roja) - Player 1's piece (red)
        2 -> Color.Yellow // Ficha del jugador 2 (amarilla) - Player 2's piece (yellow)
        else -> Color.Transparent // Sin ficha (vacío) - No piece (empty)
    }

    val bordeColor = Color(0xFF0D47A1) // Color del borde de las celdas - Color of the cell borders
    val alphaAnim = rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Diseño visual de la celda - Visual design of the cell
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