package com.example.conecta4.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.util.Log
import com.example.conecta4.model.User // Asegúrate de que esta importación sea correcta

class UsersListViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val usersRef: DatabaseReference = database.getReference("users")
    private var usersListener: ValueEventListener? = null

    init {
        setupUsersListener()
    }

    private fun setupUsersListener() {
        if (usersListener == null) {
            usersListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userList = mutableListOf<User>()
                    // PASO 1: Obtener el UID del usuario actualmente autenticado
                    val currentUserId = auth.currentUser?.uid
                    Log.d("UsersListViewModel", "Current User UID for filtering: $currentUserId") // <-- AÑADE ESTE LOG
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        // PASO 2: Asegurarse de que el usuario no sea nulo Y no sea el usuario actual
                        if (user != null && user.uid != currentUserId) {
                            userList.add(user)
                        }
                    }
                    _users.value = userList

                    Log.d("UsersListViewModel", "onDataChange: Loaded ${userList.size} users.")
                    userList.forEach { user ->
                        Log.d("UsersListViewModel", "User: ${user.username}, Status: ${user.status}, InGame: ${user.inGame}, LastOnline: ${user.lastOnline}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UsersListViewModel", "Failed to load users: ${error.message}", error.toException())
                    _users.value = emptyList()
                }
            }
            usersRef.addValueEventListener(usersListener!!)
            Log.d("UsersListViewModel", "ValueEventListener añadido a /users.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        usersListener?.let {
            usersRef.removeEventListener(it)
            Log.d("UsersListViewModel", "ValueEventListener eliminado de /users.")
        }
    }
}