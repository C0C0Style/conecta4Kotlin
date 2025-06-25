package com.example.conecta4

import android.app.Application
import android.util.Log // Importar Log para mensajes de depuración
import com.google.firebase.FirebaseApp // Importar FirebaseApp

class Conecta4 : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializa Firebase aquí
        FirebaseApp.initializeApp(this)
        Log.d("Conecta4App", "Firebase se ha inicializado correctamente.")
    }
}