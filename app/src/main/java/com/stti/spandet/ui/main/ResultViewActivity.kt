package com.stti.spandet.ui.main

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.stti.spandet.R
import com.stti.spandet.data.Repository
import com.stti.spandet.databinding.ActivityResultViewBinding
import com.stti.spandet.ui.info.AdjacentDefectFragment
import com.stti.spandet.ui.info.EmptyDetectFragment
import com.stti.spandet.ui.info.GeometryDefectFragment
import com.stti.spandet.ui.info.IntegrityDefectFragment
import com.stti.spandet.ui.info.NoDefectFragment
import com.stti.spandet.ui.info.NonpenDefectFragment
import com.stti.spandet.ui.info.PostprocDefectFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ResultViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultViewBinding
    private lateinit var repository: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityResultViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        repository = Repository(this)

        // Get the intent extras
        val resultImageUri = intent.getStringExtra("resultImage")
        val resultImageName = intent.getStringExtra("resultImageName")

        val originalImageUri = intent.getStringExtra("originalImage")
        val originalImageName = intent.getStringExtra("originalImageName")


        val isEmpty = intent.getBooleanExtra("isEmpty", false)
        val adjCount = intent.getIntExtra("adjCount", 0)
        val intCount = intent.getIntExtra("intCount", 0)
        val geoCount = intent.getIntExtra("geoCount", 0)
        val postCount = intent.getIntExtra("postCount", 0)
        val nonpenCount = intent.getIntExtra("nonCount", 0)

        val adjacentFragment = AdjacentDefectFragment.newInstance(adjCount)
        val integrityFragment = IntegrityDefectFragment.newInstance(intCount)
        val geometryFragment = GeometryDefectFragment.newInstance(geoCount)
        val postprocFragment = PostprocDefectFragment.newInstance(postCount)
        val nonpenFragment = NonpenDefectFragment.newInstance(nonpenCount)
        val emptyFragment = EmptyDetectFragment()
        val nodefectFragment = NoDefectFragment()

        if(isEmpty){
            Toast.makeText(this, "Tidak ada area pengelasan", Toast.LENGTH_SHORT).show()
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, emptyFragment)
                commit()
            }
        }
        else{
            if(adjCount>0||intCount>0||geoCount>0||postCount>0||nonpenCount>0){
               if(adjCount>0) {
                   supportFragmentManager.beginTransaction().apply {
                       add(R.id.fragment_container, adjacentFragment)
                       commit()
                   }
               }
                if(intCount>0) {
                     supportFragmentManager.beginTransaction().apply {
                          add(R.id.fragment_container, integrityFragment)
                          commit()
                     }
                }
                if(geoCount>0) {
                     supportFragmentManager.beginTransaction().apply {
                          add(R.id.fragment_container, geometryFragment)
                          commit()
                     }
                }
                if(postCount>0) {
                     supportFragmentManager.beginTransaction().apply {
                          add(R.id.fragment_container, postprocFragment)
                          commit()
                     }
                }
                if(nonpenCount>0) {
                     supportFragmentManager.beginTransaction().apply {
                          add(R.id.fragment_container, nonpenFragment)
                          commit()
                     }
                }
            }
            else{
                supportFragmentManager.beginTransaction().apply {
                    add(R.id.fragment_container, nodefectFragment)
                    commit()
                }
            }
        }

        Log.d("ResultViewActivity", "result: $resultImageUri")
        Log.d("ResultViewActivity", "original: $originalImageUri")
        Log.d("ResultViewActivity", "name: $resultImageName")


        var toggle = true

        binding.fabDetect.setOnClickListener {
            toggle = if(toggle){
                binding.ivResult.setImageURI(Uri.parse(originalImageUri))
                Log.d("ResultViwActivity", "showImage: $originalImageUri")
                false
            } else {
                binding.ivResult.setImageURI(Uri.parse(resultImageUri))
                Log.d("ResultViwActivity", "showImage: $resultImageUri")
                true
            }
        }

        binding.fabDelete.setOnClickListener {
            showDeleteConfirmationDialog(resultImageUri, originalImageUri)
        }


        // Set the image to the given URI
        resultImageUri?.let {
            Log.d("ResultViwActivity", "showImage: $it")
            binding.ivResult.setImageURI(Uri.parse(it))
        }

        // Set the image name to the TextView
        resultImageName?.let {
            binding.tvTitle.text = it
        }
    }
    private fun showDeleteConfirmationDialog(resultUri: String?, originalUri: String?) {
        AlertDialog.Builder(this).apply {
            setTitle("Konfirmasi Hapus")
            setMessage("Apakah Anda yakin ingin menghapus gambar dan semua file terkait?")
            setPositiveButton("Ya") { _, _ ->
                deleteImageFiles(resultUri, originalUri)
            }
            setNegativeButton("Tidak", null)
        }.show()
    }

    private fun deleteImageFiles(resultUri: String?, originalUri: String?) {
        if (resultUri == null || originalUri == null) {
            Log.e("ResultViewActivity", "Invalid URIs: resultUri=$resultUri, originalUri=$originalUri")
            return
        }

        val resultFile = Uri.parse(resultUri)?.path?.let { File(it) }
        val originalFile = Uri.parse(originalUri)?.path?.let { File(it) }

        val uriResult = Uri.parse(resultUri)
        val uriOriginal = Uri.parse(originalUri)

        if (resultFile != null && originalFile != null) {
            CoroutineScope(Dispatchers.Main).launch{
                val success = repository.deleteImageAndFiles(uriResult, uriOriginal)
                if (success) {
                    Toast.makeText(this@ResultViewActivity, "Files deleted successfully.", Toast.LENGTH_SHORT).show()
                    Log.d("ResultViewActivity", "Files deleted successfully.")
                    finish() // Close the activity
                } else {
                    Log.e("ResultViewActivity", "Failed to delete files.")
                }
            }
        }
    }
}