package com.stti.spandet.ui.collection

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.stti.spandet.data.Repository
import com.stti.spandet.data.preferences.UserPreferences
import com.stti.spandet.databinding.ActivitySelectCollectionBinding
import com.stti.spandet.ui.main.CollectionViewActivity
import kotlinx.coroutines.launch

class SelectCollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectCollectionBinding
    private lateinit var repository: Repository

    private lateinit var prefs : UserPreferences
    private var username = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySelectCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnNew.setOnClickListener {
            startActivity(Intent(this, CreateCollectionActivity::class.java))
            finish()
        }


        repository = Repository(this)

        prefs = UserPreferences(this)
        username = prefs.getUsername().toString()


        lifecycleScope.launch {
            val collections = repository.scanCollectionsDir(username).map { it.name }
            if (collections.isEmpty()) {
                binding.spinnerCollections.adapter = ArrayAdapter(this@SelectCollectionActivity, android.R.layout.simple_spinner_item, listOf("Pilih Koleksi"))
//                binding.btnSelect.text = "Buat Koleksi"
//                binding.btnSelect.setOnClickListener {
//                    startActivity(Intent(this@SelectCollectionActivity, CreateCollectionActivity::class.java))
//                    finish()
//                }
                binding.btnSelect.visibility = View.GONE
            } else {
                binding.spinnerCollections.adapter = ArrayAdapter(this@SelectCollectionActivity, android.R.layout.simple_spinner_item, collections)
                binding.btnSelect.text = "Pilih Koleksi"
                binding.btnSelect.setOnClickListener {
                    val selectedCollection = binding.spinnerCollections.selectedItem.toString()
                    val intent = Intent(this@SelectCollectionActivity, CollectionViewActivity::class.java)
                    intent.putExtra("collection_name", selectedCollection)
                    startActivity(intent)
                }
                binding.btnSelect.visibility = View.VISIBLE
            }
        }
    }
}