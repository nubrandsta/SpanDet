package com.stti.spandet.ui.collection

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.stti.spandet.data.Repository
import com.stti.spandet.databinding.ActivityCreateCollectionBinding
import kotlinx.coroutines.launch

class CreateCollectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateCollectionBinding
    private lateinit var repository: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCreateCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        repository = Repository(this)

        binding.etCollectionName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (input.isEmpty() || input.contains("\\W".toRegex()) || input.contains(" ")) {
                    binding.etCollectionName.error = "Perbaiki format nama koleksi!"
                    binding.btnCreate.isEnabled = false
                } else {
                    binding.etCollectionName.error = null
                    binding.btnCreate.isEnabled = true
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnCreate.setOnClickListener {
            val name = binding.etCollectionName.text.toString()
            // implement coroutine scope

            val timestamp = System.currentTimeMillis()

            lifecycleScope.launch {
                val existingCollections = repository.scanCollectionsDir().map { it.name }
                if (name in existingCollections) {
                    AlertDialog.Builder(this@CreateCollectionActivity)
                        .setMessage("Sebuah koleksi dengan nama ini sudah ada!.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    repository.createCollectionDir(name,timestamp)
                    Toast.makeText(this@CreateCollectionActivity, "Koleksi berhasil dibuat!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}