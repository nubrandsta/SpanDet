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
import com.stti.spandet.ui.details.EmptyDetectFragment
import com.stti.spandet.ui.details.SpandukFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ResultViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultViewBinding
    private lateinit var repository: Repository

    private var latitude : Double = 0.0
    private var longitude : Double = 0.0
    private var timestamp : Long = 0L

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
        val spandukCount = intent.getIntExtra("spandukCount", 0)
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)
        timestamp = intent.getLongExtra("timestamp", 0L)

        val emptyFragment = EmptyDetectFragment()
        val spandukFragment = SpandukFragment.newInstance(spandukCount, latitude, longitude, timestamp)



        if(isEmpty){
            Toast.makeText(this, "Tidak ada spanduk dalam gambar", Toast.LENGTH_SHORT).show()
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, spandukFragment)
                commit()
            }
        }
        else{
           supportFragmentManager.beginTransaction().apply {
               add(R.id.fragment_container, spandukFragment)
               commit()
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

        binding.fabBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
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