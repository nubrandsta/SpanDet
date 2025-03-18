package com.stti.spandet.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.stti.spandet.data.model.ClassOccurence
import com.stti.spandet.data.model.Collection
import com.stti.spandet.data.model.ProcessImage
import com.stti.spandet.data.model.ResultImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class Repository(private val context: Context) {

    private val TAG = "Repository"

    // Check or create the collections directory
    fun checkForCollectionsDir(): File? {
        val collectionsDir = File(context.filesDir, "collections")
        return if (!collectionsDir.exists()) {
            val created = collectionsDir.mkdir()
            if (created) {
                Log.d(TAG, "Collections directory created successfully.")
                collectionsDir
            } else {
                Log.e(TAG, "Failed to create collections directory.")
                null
            }
        } else {
            collectionsDir
        }
    }

    // Scan the collections directory for existing collections
    suspend fun scanCollectionsDir(): List<Collection> = withContext(Dispatchers.IO) {
        val collectionsDir = checkForCollectionsDir()
        if (collectionsDir != null && collectionsDir.exists()) {
            val directories = collectionsDir.listFiles()?.filter { dir ->
                dir.isDirectory && File(dir, "image").listFiles()?.isNotEmpty() == true
            }

            Log.d(TAG, "Found non-empty directories: ${directories?.map { it.name }}")

            directories?.map { dir ->
                val name = dir.name
                val imgCount = File(dir, "image").listFiles()?.size ?: 0
                val metadataFile = File(dir, "metadata.json")

                val location = if (metadataFile.exists()) {
                    val metadataJson = JSONObject(metadataFile.readText())
                    metadataJson.optString("location", "Unknown") // Default to "Unknown" if missing
                } else {
                    "Unknown"
                }

                val lat = if (metadataFile.exists()) {
                    val metadataJson = JSONObject(metadataFile.readText())
                    metadataJson.optDouble("lat", 0.0) // Default to 0.0 if missing
                } else {
                    0.0

                }

                val lon = if (metadataFile.exists()) {
                    val metadataJson = JSONObject(metadataFile.readText())
                    metadataJson.optDouble("lon", 0.0) // Default to 0.0 if missing
                } else {
                    0.0
                }

                Collection(name, imgCount, location, lat, lon)  // Include location in Collection model
            } ?: emptyList()
        } else {
            Log.d(TAG, "Collections directory does not exist.")
            emptyList()
        }
    }

    suspend fun getCollectionMetadata(collectionName: String): Collection? = withContext(Dispatchers.IO) {
        val collectionsDir = checkForCollectionsDir()
        if (collectionsDir != null && collectionsDir.exists()) {
            val collectionDir = File(collectionsDir, collectionName)
            val metadataFile = File(collectionDir, "metadata.json")

            if (collectionDir.exists() && collectionDir.isDirectory && metadataFile.exists()) {
                val metadataJson = JSONObject(metadataFile.readText())
                val name = metadataJson.optString("name", collectionName)
                val location = metadataJson.optString("locationString", "Unknown")
                val lat = metadataJson.optDouble("lat", 0.0)
                val lon = metadataJson.optDouble("lon", 0.0)
                val imgCount = File(collectionDir, "image").listFiles()?.size ?: 0

                return@withContext Collection(name, imgCount, location, lat, lon)
            } else {
                Log.d(TAG, "Collection $collectionName or its metadata.json does not exist.")
            }
        }
        return@withContext null
    }


    // Create a new collection directory along with 'image' and 'result' subdirectories
    fun createCollectionDir(name: String, locationString: String="none", lat: Double=0.0, lon: Double=0.0): Boolean {
        val collectionsDir = checkForCollectionsDir()
        if (collectionsDir == null) {
            Log.e(TAG, "Cannot create collection $name: Collections directory does not exist and could not be created.")
            return false
        }

        val collectionDir = File(collectionsDir, name)
        if (!collectionDir.exists()) {
            val created = collectionDir.mkdir()
            if (created) {
                val imageDirCreated = File(collectionDir, "image").mkdir()
                val resultDirCreated = File(collectionDir, "result").mkdir()

                if (imageDirCreated && resultDirCreated) {
                    // **Save metadata JSON**
                    val metadataFile = File(collectionDir, "metadata.json")
                    val metadataJson = JSONObject().apply {
                        put("name", name)
                        put("locationString", locationString)
                        put("lat", lat)
                        put("lon", lon)
                        put("imgCount", 0)
                    }
                    metadataFile.writeText(metadataJson.toString())

                    Log.d(TAG, "Collection $name created successfully with image, result directories, and metadata.json.")
                    return true
                } else {
                    Log.e(TAG, "Failed to create subdirectories for collection $name.")
                    collectionDir.deleteRecursively() // Clean up if creation failed
                    return false
                }
            } else {
                Log.e(TAG, "Failed to create collection directory $name.")
                return false
            }
        } else {
            Log.d(TAG, "Collection directory $name already exists.")
            return false
        }
    }


    suspend fun scanImages(collectionName: String): List<ProcessImage> = withContext(Dispatchers.IO) {
        val collectionsDir = checkForCollectionsDir()
        val processImages = mutableListOf<ProcessImage>()

        if (collectionsDir != null && collectionsDir.exists()) {
            val collectionDir = File(collectionsDir, collectionName)

            if (collectionDir.exists() && collectionDir.isDirectory) {
                val imageDir = File(collectionDir, "image")

                if (imageDir.exists() && imageDir.isDirectory) {
                    imageDir.listFiles()?.filter { it.isFile && it.extension in listOf("jpg", "jpeg", "png") }?.forEach { imageFile ->
                        val uri = Uri.fromFile(imageFile)
                        val processImage = ProcessImage(
                            uri = uri
                        )
                        processImages.add(processImage)
                    }
                } else {
                    Log.d(TAG, "Image directory does not exist or is not a directory.")
                }
            } else {
                Log.d(TAG, "Collection directory does not exist or is not a directory.")
            }
        } else {
            Log.d(TAG, "Collections directory does not exist.")
        }

        return@withContext processImages
    }

    suspend fun scanResult(collectionName: String): List<ResultImage> = withContext(Dispatchers.IO) {
        val collectionsDir = checkForCollectionsDir()
        val resultImages = mutableListOf<ResultImage>()

        if (collectionsDir != null && collectionsDir.exists()) {
            val collectionDir = File(collectionsDir, collectionName)

            if (collectionDir.exists() && collectionDir.isDirectory) {
                val resultDir = File(collectionDir, "result")
                val imageDir = File(collectionDir, "image")

                if (resultDir.exists() && resultDir.isDirectory) {
                    val imageFiles = resultDir.listFiles()?.filter {
                        it.isFile && it.extension in listOf("jpg", "jpeg", "png")
                    } ?: emptyList()

                    imageFiles.forEach { imageFile ->
                        val baseName = imageFile.nameWithoutExtension
                        val jsonFile = File(resultDir, "$baseName.json")

                        // Extract the timestamp from the baseName
                        val timestamp = baseName.substringAfterLast('_')

                        // Correctly locate the original image
                        val originalImageFile = imageDir.listFiles()?.firstOrNull {
                            it.name.contains("_original_$timestamp")
                        }

                        val originalUri = if (originalImageFile != null && originalImageFile.exists()) {
                            Uri.fromFile(originalImageFile)
                        } else {
                            Uri.fromFile(imageFile)  // Fallback to the result image if no original found
                        }

                        if (jsonFile.exists()) {
                            val jsonContent = jsonFile.readText()
                            val classOccurence = parseClassOccurrencesFromJson(jsonContent)
                            val resultImage = ResultImage(
                                fileName = imageFile.name,
                                fileSize = imageFile.length(),
                                uri = Uri.fromFile(imageFile),
                                originalUri = originalUri,
                                isEmpty = false,
                                classOccurence = classOccurence
                            )
                            resultImages.add(resultImage)
                        } else {
                            // JSON file not found, mark as empty workpiece
                            val resultImage = ResultImage(
                                fileName = imageFile.name,
                                fileSize = imageFile.length(),
                                uri = Uri.fromFile(imageFile),
                                originalUri = originalUri,
                                isEmpty = true
                            )
                            resultImages.add(resultImage)
                        }
                    }
                } else {
                    Log.d(TAG, "Result directory does not exist or is not a directory.")
                }
            } else {
                Log.d(TAG, "Collection directory does not exist or is not a directory.")
            }
        } else {
            Log.d(TAG, "Collections directory does not exist.")
        }

        return@withContext resultImages
    }


    private fun parseClassOccurrencesFromJson(jsonContent: String): ClassOccurence {
        val jsonObject = JSONObject(jsonContent)
        val detections = jsonObject.getJSONArray("detections")

        var adjCount = 0
        var intCount = 0
        var geoCount = 0
        var proCount = 0
        var nonCount = 0

        for (i in 0 until detections.length()) {
            val detection = detections.getJSONObject(i)
            val className = detection.getString("class_name")

            when (className) {
                "adj" -> adjCount++
                "int" -> intCount++
                "geo" -> geoCount++
                "pro" -> proCount++
                "non" -> nonCount++
            }
        }

        return ClassOccurence(
            adj = adjCount,
            int = intCount,
            geo = geoCount,
            pro = proCount,
            non = nonCount
        )
    }

    suspend fun deleteImageAndFiles(resultUri: Uri, originalUri: Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Get the corresponding files
            val resultFile = File(resultUri.path ?: "")
            val originalFile = File(originalUri.path ?: "")
            val jsonFile = File(resultFile.parent, resultFile.nameWithoutExtension + ".json")

            var isSuccess = true

            // Delete the result image file
            if (resultFile.exists() && resultFile.delete()) {
                Log.d(TAG, "Result image deleted: ${resultFile.absolutePath}")
            } else {
                Log.e(TAG, "Failed to delete result image: ${resultFile.absolutePath}")
                isSuccess = false
            }

            // Delete the JSON file
            if (jsonFile.exists() && jsonFile.delete()) {
                Log.d(TAG, "Associated JSON deleted: ${jsonFile.absolutePath}")
            } else {
                Log.e(TAG, "Failed to delete associated JSON: ${jsonFile.absolutePath}")
                isSuccess = false
            }

            // Delete the original image file
            if (originalFile.exists() && originalFile.delete()) {
                Log.d(TAG, "Original image deleted: ${originalFile.absolutePath}")
            } else {
                Log.e(TAG, "Failed to delete original image: ${originalFile.absolutePath}")
                isSuccess = false
            }

            isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting files: ${e.message}", e)
            false
        }
    }



}
