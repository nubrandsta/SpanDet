package com.stti.spandet.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.stti.spandet.R
import com.stti.spandet.data.Repository
import com.stti.spandet.data.model.ClassOccurence
import com.stti.spandet.databinding.ActivityCollectionViewBinding
import com.stti.spandet.ui.collection.ProcessActivity
import com.stti.spandet.ui.main.adapters.resultListAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

class CollectionViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectionViewBinding
    private lateinit var adapter: resultListAdapter
    private lateinit var repository: Repository

    private var collectionName: String? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var timestamp: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCollectionViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        repository = Repository(this)

        val collection_name = intent.getStringExtra("collection_name") // replace with your actual key
        collectionName = collection_name

        lifecycleScope.launch {
            val collection = repository.getCollectionMetadata(collection_name!!)
            if (collection != null) {
                Log.d("Process", "Collection Metadata: $collection")
                binding.tvDate.text = collection.locationString

                latitude = collection.lat
                longitude = collection.lon
                timestamp = collection.timestamp
            } else {
                Toast.makeText(this@CollectionViewActivity, "Collection not found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvSubheading.text = collection_name

        // Initialize the repository
        repository = Repository(this)

        binding.fabDetect.setOnClickListener {
            val intent = Intent(this, ProcessActivity::class.java)
            intent.putExtra("collection_name", collection_name)
            startActivity(intent)
        }

        binding.fabBack.setOnClickListener {
            finish()
        }

        // Initialize the adapter
        adapter = resultListAdapter { resultImage ->
            // intent to ResultViewActivity
            val intent = Intent(this, ResultViewActivity::class.java)
            intent.putExtra("collection_name", collection_name)
            intent.putExtra("resultImage", resultImage.uri.toString())

            intent.putExtra("resultImageName", resultImage.fileName)
            intent.putExtra("originalImage", resultImage.originalUri.toString())

            intent.putExtra("isEmpty", resultImage.isEmpty)
            intent.putExtra("spandukCount", resultImage.classOccurence.spanduk)

            intent.putExtra("latitude", latitude)
            intent.putExtra("longitude", longitude)
            intent.putExtra("timestamp", timestamp)

            startActivity(intent)
        }

        // Get the RecyclerView and set its adapter and layout manager
        val recyclerView = binding.rvResult // replace with your actual RecyclerView id
        recyclerView.adapter = adapter
        val numberOfColumns = 2
        recyclerView.layoutManager = GridLayoutManager(this, numberOfColumns)

        // Populate the RecyclerView with images
        CoroutineScope(Dispatchers.Main).launch {
            showProgressBar()
            val collectionName = collection_name // replace with your actual collection name
            val images = collectionName?.let { repository.scanResult(it) }
            adapter.submitList(images)
            hideProgressBar()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the RecyclerView when the activity resumes
        refreshRecyclerView()
    }

    private fun refreshRecyclerView() {
        CoroutineScope(Dispatchers.Main).launch {
            showProgressBar()
            adapter.submitList(emptyList())
            val images = collectionName?.let { repository.scanResult(it) }
            adapter.submitList(images)
            hideProgressBar()
        }
    }

    private suspend fun exportResultsToExcel() = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(collectionName ?: "Report"))

            // Create header row
            val headerRow = sheet.createRow(0)
            val headers = listOf(
                "Gambar Asli", "Deteksi", "Deteksi Area Las", "Status",
                "Jumlah Cacat adj", "Jumlah Cacat int", "Jumlah Cacat geo", "Jumlah Cacat non", "Jumlah Cacat pro"
            )
            headers.forEachIndexed { index, title ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(title)
            }

            // Fetch result images
            val resultImages = collectionName?.let { repository.scanResult(it) } ?: emptyList()

            // Populate rows with data
            resultImages.forEachIndexed { index, resultImage ->
                val row = sheet.createRow(index + 1)

                // Gambar Asli
                val originalFile = File(resultImage.originalUri.path ?: "")
                row.createCell(0).setCellValue(originalFile.name)

                // Deteksi
                val resultFile = File(resultImage.uri.path ?: "")
                row.createCell(1).setCellValue(resultFile.name)

                // Deteksi Area Las
                row.createCell(2).setCellValue(if (resultImage.isEmpty) "Tidak" else "Ya")

                // Status
                row.createCell(3).setCellValue(if (resultImage.classOccurence == ClassOccurence()) "OK" else "Cacat")

                // Jumlah Cacat columns
                row.createCell(4).setCellValue(resultImage.classOccurence.spanduk.toDouble())
            }

            // Save the workbook to external storage
            val timeNow = System.currentTimeMillis()
            val fileName = "${collectionName ?: "Report"}_$timeNow.xlsx"
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            FileOutputStream(file).use { out ->
                workbook.write(out)
            }
            workbook.close()

            // Create the URI using FileProvider
            val uri = FileProvider.getUriForFile(
                this@CollectionViewActivity,
                "${applicationContext.packageName}.provider",
                file
            )

            // Open the file with an intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)

        } catch (e: Exception) {
            Log.e("ExportError", "Error exporting results to Excel: ${e.message}", e)
        }
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }
}
