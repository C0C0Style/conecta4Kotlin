package com.example.conecta4 // Asegúrate de que este sea el paquete correcto

import android.app.Application
import com.google.firebase.FirebaseApp
import android.util.Log // Importa Log para depuración

class Conecta4 : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("FirebaseInit", "Intentando inicializar Firebase en Conecta4.onCreate()") // Log para depuración
        try {
            FirebaseApp.initializeApp(this)
            Log.d("FirebaseInit", "Firebase inicializado con ÉXITO.")
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Error al inicializar Firebase: ${e.message}", e)
        }
    }
}