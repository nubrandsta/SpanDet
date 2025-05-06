package com.stti.spandet.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.stti.spandet.R
import com.stti.spandet.data.api.injection.ViewModelFactory
import com.stti.spandet.data.preferences.UserPreferences
import com.stti.spandet.databinding.ActivityLoginBinding
import com.stti.spandet.data.api.ResultState
import com.stti.spandet.ui.home.HomeActivity
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher


class LoginActivity : AppCompatActivity() {

    private lateinit var binding : ActivityLoginBinding
    private lateinit var prefs: UserPreferences

    private lateinit var factory: ViewModelFactory
    private val viewModel by viewModels<LoginViewModel> {
        ViewModelFactory.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        factory = ViewModelFactory.getInstance()

        prefs = UserPreferences(this)
        prefs.clear()

        checkInput()

        binding.btnLogin.setOnClickListener {
            val usernameInput = binding.etUsername.text?.trim().toString()
            val passwordInput = binding.etPassword.text?.trim().toString()

            // Only proceed with login if both fields are not empty
            if(!usernameInput.isEmpty() && !passwordInput.isEmpty()){
                binding.progressBar.visibility = View.VISIBLE
                viewModel.login(usernameInput, passwordInput).observe(this){result ->
                    if(result !=null){
                        when(result){
                            is ResultState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this, "Selamat Datang, ${result.data.fullName}", Toast.LENGTH_SHORT).show()
                                prefs.saveLogin(result.data.username.toString(), result.data.session.toString(), result.data.fullName.toString(), result.data.group.toString())
                                val intent = Intent(this, HomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            is ResultState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this, result.errorMessage, Toast.LENGTH_SHORT).show()
                            }
                            is ResultState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Username dan password harus diisi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkInput(){
        //trigger validate input when edit text is changed
        binding.etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validateInput() }
        })
        
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validateInput() }
        })
        
        // Initial validation
        validateInput()
    }

    private fun validateInput(){
        val usernameInput = binding.etUsername.text?.trim().toString()
        val passwordInput = binding.etPassword.text?.trim().toString()

        // Default to disabled until validation passes
        binding.btnLogin.isEnabled = false

        when {
            usernameInput.isEmpty() -> {
                binding.etUsername.error = "Username harus diisi"
                binding.etPassword.error = null
            }
            passwordInput.isEmpty() -> {
                binding.etUsername.error = null
                binding.etPassword.error = "Password harus diisi"
            }
            else -> {
                binding.etUsername.error = null
                binding.etPassword.error = null
                binding.btnLogin.isEnabled = true
            }
        }
    }
}