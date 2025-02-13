package com.stti.spandet.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.stti.spandet.data.Repository
import com.stti.spandet.databinding.ActivityMainBinding
import com.stti.spandet.ui.collection.CreateCollectionActivity
import com.stti.spandet.ui.collection.SelectCollectionActivity
import com.stti.spandet.ui.main.adapters.collectionListAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var adapter: collectionListAdapter
    private lateinit var repository: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        repository = Repository(this)

        adapter = collectionListAdapter { collection ->
            // intent to collection view activity
            val selectedCollection = collection.name
            val intent = Intent(this, CollectionViewActivity::class.java)
            intent.putExtra("collection_name", selectedCollection)
            startActivity(intent)
        }

        binding.btnCreateCollection.setOnClickListener {
            // intent to create collection activity
            val intent = Intent(this, CreateCollectionActivity::class.java)
            startActivity(intent)
        }

        binding.fabDetect.setOnClickListener {
            // intent to select collection activity
            val intent = Intent(this, SelectCollectionActivity::class.java)
            startActivity(intent)
        }

        binding.rvCollection.layoutManager = LinearLayoutManager(this)
        binding.rvCollection.adapter = adapter

        lifecycleScope.launch {
            val collections = repository.scanCollectionsDir()
            if (collections.isEmpty()) {
                binding.rvCollection.visibility = View.GONE
                binding.emptyPrompt.visibility = View.VISIBLE
            } else {
                binding.rvCollection.visibility = View.VISIBLE
                binding.emptyPrompt.visibility = View.GONE
                adapter.submitList(collections)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val collections = repository.scanCollectionsDir()
            if (collections.isEmpty()) {
                binding.rvCollection.visibility = View.GONE
                binding.emptyPrompt.visibility = View.VISIBLE
            } else {
                binding.rvCollection.visibility = View.VISIBLE
                binding.emptyPrompt.visibility = View.GONE
                adapter.submitList(collections)
            }
        }
    }
}