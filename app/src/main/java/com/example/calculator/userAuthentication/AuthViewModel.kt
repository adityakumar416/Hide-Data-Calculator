package com.example.calculator.userAuthentication

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AuthViewModel : ViewModel(){

    private val auth = FirebaseAuth.getInstance()
    val loginResult: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    private val database = FirebaseDatabase.getInstance()
    private val userNameFatch: DatabaseReference = database.getReference("users")
    val userData: MutableLiveData<UserModal> by lazy {
        MutableLiveData<UserModal>()
    }

    fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    loginResult.postValue(true)
                } else {
                    loginResult.postValue(false)
                }
            }
    }

    fun registerUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""
                    val userModal = UserModal(uid, name, email, password)
                    userNameFatch.child(uid).setValue(userModal)
                    loginResult.postValue(true)
                } else {
                    loginResult.postValue(false)
                }
            }
    }

    fun fetchUser() {
        userNameFatch.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    val userModal = postSnapshot.getValue(UserModal::class.java)
                    // Here you can add checks to see if the user exists or not
                    userData.postValue(userModal)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                userData.postValue(null)
            }
        })
    }

}