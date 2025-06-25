package com.example.conecta4

import AppNavigation
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
import com.example.conecta4.view.ViewInicioSesion
import com.example.conecta4.view.ViewRegistro
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// Actividad principal que maneja la interfaz de usuario - Main activity that handles the user interface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Conecta4Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // PantallaPrincipal() // Pantalla principal del juego - Main game screen
                    AppNavigation()
                }
            }
        }
    }
}
/*
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

/*
// Pantalla principal que maneja el estado del juego - Main screen that manages the game state

@Composable

fun PantallaPrincipal() {

// Inicialización del tablero con 6 filas y 7 columnas (tablero de Conecta 4) - Initialize the board with 6 rows and 7 columns (Connect 4 board)

    val tablero = remember { List(6) { mutableStateListOf(*Array(7) { 0 }) } }

// Lista que guarda las animaciones de las fichas que caen en el tablero - List that stores the animations of the falling pieces

    val posicionesAnimadas = remember { List(6) { MutableList(7) { Animatable(-300f) } } }



// Estado que gestiona el turno del jugador y otros estados del juego - State that manages the player's turn and other game states

    var turnoJugador by remember { mutableStateOf(1) }

    var mensaje by remember { mutableStateOf("Tu turno 🔴") }

    var juegoFinalizado by remember { mutableStateOf(false) }

    var celdasGanadoras by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }

    var puedeJugar by remember { mutableStateOf(true) }



// Scope de corutinas para manejar animaciones y lógica asíncrona - Coroutine scope to handle animations and asynchronous logic

    val scope = rememberCoroutineScope()



// Función para reiniciar el juego - Function to restart the game

    fun reiniciarJuego() {

        for (fila in 0..5) {

            for (col in 0..6) {

                tablero[fila][col] = 0 // Limpiar el tablero - Clear the board

                posicionesAnimadas[fila][col] = Animatable(-300f) // Reiniciar las animaciones - Reset the animations

            }

        }

        turnoJugador = 1

        mensaje = "Tu turno 🔴"

        juegoFinalizado = false

        celdasGanadoras = emptyList()

        puedeJugar = true

    }



// Función que maneja el turno de un jugador - Function that handles a player's turn

    fun jugarTurno(col: Int, jugador: Int) {

        puedeJugar = false

        val filaDisponible = buscarFilaDisponible(tablero, col) // Busca la fila donde se puede colocar la ficha - Find the available row to place the piece

        if (filaDisponible != -1) { // Si hay una fila disponible - If there's an available row

            scope.launch {

                tablero[filaDisponible][col] = jugador // Coloca la ficha en el tablero - Place the piece on the board

// Animación de la ficha que cae - Animation of the falling piece

                posicionesAnimadas[filaDisponible][col].snapTo(-300f)

                posicionesAnimadas[filaDisponible][col].animateTo(

                    targetValue = 0f,

                    animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)

                )



// Verificar si hay un ganador después de colocar la ficha - Check if there's a winner after placing the piece

                val ganadoras = verificarGanador(tablero, jugador)

                if (ganadoras.isNotEmpty()) { // Si hay un ganador - If there's a winner

                    celdasGanadoras = ganadoras

                    val emoji = if (jugador == 1) "🔴" else "🟡"

                    mensaje = "🎉 ¡${if (jugador == 1) "Tú ganas" else "La máquina gana"} ! $emoji" // Mensaje de ganador - Winner message

                    juegoFinalizado = true

                } else if (tablero.all { fila -> fila.all { it != 0 } }) { // Si el tablero está lleno (empate) - If the board is full (draw)

                    mensaje = "Empate 🤝"

                    juegoFinalizado = true

                } else { // Si el juego sigue - If the game continues

                    turnoJugador = if (jugador == 1) 2 else 1

                    mensaje = if (turnoJugador == 1) "Tu turno 🔴" else "Turno de la máquina 🟡"



// Si es el turno de la máquina, realiza su jugada automáticamente - If it's the machine's turn, it plays automatically

                    if (turnoJugador == 2 && !juegoFinalizado) {

                        delay(800)

                        val colMaquina = elegirJugadaMaquina(tablero) // Elige la columna para la máquina - Choose the column for the machine

                        jugarTurno(colMaquina, 2) // La máquina realiza su jugada - The machine makes its move

                    } else if (turnoJugador == 1 && !juegoFinalizado) {

                        puedeJugar = true

                    }

                }

            }

        } else {

            puedeJugar = true

        }

    }



// Diseño de la interfaz con el tablero y los controles del juego - UI design with the board and game controls

    Column(

        modifier = Modifier

            .fillMaxSize()

            .padding(16.dp),

        verticalArrangement = Arrangement.Center,

        horizontalAlignment = Alignment.CenterHorizontally

    ) {

        Text("¡\uD83D\uDC7E Bienvenido \uD83D\uDC7E! ", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 32.sp)) // Título del juego - Game title

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = mensaje, fontSize = 18.sp)

        Spacer(modifier = Modifier.height(32.dp))



// Mostrar el tablero y permitir la interacción - Display the board and allow interaction

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



// Mostrar botón para reiniciar el juego si está finalizado - Show a button to restart the game if it's finished

        if (juegoFinalizado) {

            Button(onClick = { reiniciarJuego() }) {

                Text("Reiniciar Juego")

            }

        }

    }

}



// Función que busca la primera fila disponible en una columna - Function that finds the first available row in a column

fun buscarFilaDisponible(tablero: List<MutableList<Int>>, col: Int): Int {

    for (fila in tablero.indices.reversed()) {

        if (tablero[fila][col] == 0) return fila // Devuelve la primera fila libre - Return the first available row

    }

    return -1 // Si no hay fila disponible, devuelve -1 - If no row is available, return -1

}



// Función que elige la jugada de la máquina - Function that chooses the machine's move

fun elegirJugadaMaquina(tablero: List<MutableList<Int>>): Int {

    val columnasDisponibles = (0..6).filter { col -> buscarFilaDisponible(tablero, col) != -1 }

    return columnasDisponibles.random() // Elige una columna al azar de las disponibles - Choose a random available column

}



// Componente que representa el tablero de juego - Component that represents the game board

@Composable

fun Tablero(

    tablero: List<MutableList<Int>>,

    posicionesAnimadas: List<List<Animatable<Float, AnimationVector1D>>>,

    celdasGanadoras: List<Pair<Int, Int>>,

    onColClick: (Int) -> Unit

) {

    val outerPad = 16.dp // margen externo más amplio - wider outer margin

    val innerPad = 6.dp // espacio entre celdas/filas - space between cells/rows



// Sección del tablero, que contiene las filas y columnas de celdas - Board section containing the rows and columns of cells

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



// Componente que representa una celda del tablero - Component that represents a board cell





// Función que verifica si hay un ganador en el tablero - Function that checks if there's a winner on the board

fun verificarGanador(tablero: List<MutableList<Int>>, jugador: Int): List<Pair<Int, Int>> {

// Horizontal, Vertical and Diagonal checks for a Connect 4 winner

    for (row in 0 until 6) {

        for (col in 0 until 7) {

            if (col + 3 < 7 && (0 until 4).all { i -> tablero[row][col + i] == jugador }) {

                return (0 until 4).map { i -> row to col + i } // Horizontal win - Victoria horizontal

            }

            if (row + 3 < 6 && (0 until 4).all { i -> tablero[row + i][col] == jugador }) {

                return (0 until 4).map { i -> row + i to col } // Vertical win - Victoria vertical

            }

            if (row + 3 < 6 && col + 3 < 7 && (0 until 4).all { i -> tablero[row + i][col + i] == jugador }) {

                return (0 until 4).map { i -> row + i to col + i } // Diagonal win (descendente) - Diagonal win (descending)

            }

            if (row - 3 >= 0 && col + 3 < 7 && (0 until 4).all { i -> tablero[row - i][col + i] == jugador }) {

                return (0 until 4).map { i -> row - i to col + i } // Diagonal win (ascendente) - Diagonal win (ascending)

            }

        }

    }

    return emptyList() // Si no hay ganador - If no winner

}
*/