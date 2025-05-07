package com.stti.spandet.ui.init

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stti.spandet.data.api.ResultState
import com.stti.spandet.data.api.injection.ViewModelFactory
import com.stti.spandet.data.preferences.UserPreferences
import com.stti.spandet.databinding.ActivitySplashBinding
import com.stti.spandet.ui.auth.LoginActivity
import com.stti.spandet.ui.home.HomeActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySplashBinding

    private lateinit var factory : ViewModelFactory
    private val viewModel by viewModels<SplashViewModel> {
        ViewModelFactory.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        factory = ViewModelFactory.getInstance()

        // Get external storage permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Get external storage permission
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }

//        // Get camera permission
//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 1)
//        }

        // Check if token exists and validate it
        val prefs = UserPreferences(this)
        val token = prefs.getToken()
        
        if (token != null) {
            // Token exists, validate it
            validateToken(token)
        } else {
            // No token, go to login after delay
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToLogin()
            }, 1000) // 2 seconds delay
        }
    }
    
    private fun validateToken(token: String) {
        
        viewModel.validate(token).observe(this) { result ->
            if(result!=null){
                when(result){
                    is ResultState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        navigateToHome()
                    }
                    is ResultState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, result.errorMessage, Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    }
                    is ResultState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }

        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        Toast.makeText(this, "Sesi kadaluarsa, tolong login ulang", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}