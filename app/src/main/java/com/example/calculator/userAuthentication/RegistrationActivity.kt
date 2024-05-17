package com.example.calculator.userAuthentication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.calculator.MainActivity
import com.example.calculator.databinding.ActivityRegistrationBinding
import com.example.calculator.userAuthentication.utlis.PrefManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var prefManager: PrefManager
    private lateinit var auth: FirebaseAuth
    private lateinit var authViewModel: AuthViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        auth = Firebase.auth
        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        binding.signIn.setOnClickListener {

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

        }

        prefManager = PrefManager(this)



        binding.signUpBtn.setOnClickListener {

            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (binding.nameEditText.text!!.isEmpty()) {
                binding.nameEditText.requestFocus()
                Snackbar.make(binding.nameEditText, "Name is Mandatory.", Snackbar.LENGTH_SHORT)
                    .show();

            } else if (binding.emailEditText.text!!.isEmpty()) {
                binding.emailEditText.requestFocus()
                Snackbar.make(binding.emailEditText, "Email is Mandatory.", Snackbar.LENGTH_SHORT)
                    .show();
            } else if (binding.passwordEditText.text!!.isEmpty()) {
                binding.passwordEditText.requestFocus()
                Snackbar.make(
                    binding.passwordEditText,
                    "Password is Mandatory.",
                    Snackbar.LENGTH_SHORT
                ).show();
            } else if (binding.confirmPasswordEditText.text!!.isEmpty()) {
                binding.confirmPasswordEditText.requestFocus()
                Snackbar.make(
                    binding.confirmPasswordEditText,
                    "Confirm Password is Mandatory.",
                    Snackbar.LENGTH_SHORT
                ).show();
            } else {

                binding.progressBar.visibility = View.VISIBLE
                binding.signUpBtn.visibility = View.GONE
                authViewModel.loginResult.observe(this, loginResult)
                authViewModel.registerUser(name, email, password)

            }

        }

    }

    var loginResult = Observer<Boolean> { success ->
        if (success) {
            binding.progressBar.visibility = View.GONE
            binding.signUpBtn.visibility = View.VISIBLE
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            binding.progressBar.visibility = View.GONE
            binding.signUpBtn.visibility = View.VISIBLE
            Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
        }
    }


}