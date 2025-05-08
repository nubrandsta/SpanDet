package com.stti.spandet.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.stti.spandet.R
import com.stti.spandet.data.api.ResultState
import com.stti.spandet.data.api.injection.ViewModelFactory
import com.stti.spandet.databinding.ActivityChangePasswordBinding
import com.stti.spandet.databinding.ActivityLoginBinding
import com.stti.spandet.ui.home.HomeActivity
import android.text.Editable
import android.text.TextWatcher

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding : ActivityChangePasswordBinding

    private lateinit var factory: ViewModelFactory
    private val viewModel by viewModels<ChangePasswordViewModel> {
        ViewModelFactory.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        factory = ViewModelFactory.getInstance()

        checkInput()

        binding.btnSubmit.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val oldPassword = binding.etOldPassword.text.toString()
            val newPassword = binding.etNewPassword.text.toString()

            viewModel.changePassword(username, oldPassword, newPassword).observe(this){result ->
                if(result !=null){
                    when(result){
                        is ResultState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(this, "Password berhasil diubah", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, LoginActivity::class.java)
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
        }


    }

    private fun checkInput(){
        binding.etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validateInput() }
        })
        
        binding.etOldPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validateInput() }
        })
        
        binding.etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validateInput() }
        })
        
        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validateInput() }
        })
        
        // Initial validation
        validateInput()
    }

    private fun validateInput(){
        val usernameInput = binding.etUsername.text?.trim().toString()
        val oldPasswordInput = binding.etOldPassword.text?.trim().toString()
        val newPasswordInput = binding.etNewPassword.text?.trim().toString()
        val confirmPasswordInput = binding.etConfirmPassword.text?.trim().toString()

        // Default to disabled until validation passes
        binding.btnSubmit.isEnabled = false
        
        // Clear all errors first
        binding.etUsername.error = null
        binding.etOldPassword.error = null
        binding.etNewPassword.error = null
        binding.etConfirmPassword.error = null
        
        // Check each field
        var isValid = true
        
        if(usernameInput.isEmpty()){
            binding.etUsername.error = "Username tidak boleh kosong"
            isValid = false
        }
        
        if(oldPasswordInput.isEmpty()){
            binding.etOldPassword.error = "Password lama harus diisi"
            isValid = false
        }
        
        if(newPasswordInput.isEmpty()){
            binding.etNewPassword.error = "Password baru harus diisi"
            isValid = false
        }
        else if(newPasswordInput.length < 8){
            binding.etNewPassword.error = "Password baru minimal 8 karakter"
            isValid = false
        }
        
        if(confirmPasswordInput.isEmpty()){
            binding.etConfirmPassword.error = "Konfirmasi password harus diisi"
            isValid = false
        } else if(confirmPasswordInput != newPasswordInput){
            binding.etConfirmPassword.error = "Password baru tidak sama"
            isValid = false
        }
        
        // Enable button only if all validations pass
        binding.btnSubmit.isEnabled = isValid
    }
}