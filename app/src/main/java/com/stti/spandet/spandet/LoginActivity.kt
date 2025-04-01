package com.stti.spandet.spandet

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.stti.spandet.R
import com.stti.spandet.data.preferences.UserPreferences
import com.stti.spandet.databinding.ActivityHomeBinding
import com.stti.spandet.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding : ActivityLoginBinding
    private lateinit var prefs: UserPreferences

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

        prefs = UserPreferences(this)
        prefs.clear()

        binding.btnLogin.setOnClickListener {
            val usernameInput = binding.etUsername.text?.trim().toString()
            if(usernameInput.isNotEmpty()){
                prefs.saveLogin(usernameInput, "token")
                Toast.makeText(this, "Selamat Datang ${usernameInput}", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
            }


        }


    }
}