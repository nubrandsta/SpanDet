package com.stti.spandet.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
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
import java.util.Locale

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
    suspend fun scanCollectionsDir(currentUser: String): List<Collection> = withContext(Dispatchers.IO) {
        val collectionsDir = checkForCollectionsDir()
        if (collectionsDir != null && collectionsDir.exists()) {
            val directories = collectionsDir.listFiles()?.filter { dir ->
                dir.isDirectory && File(dir, "image").listFiles()?.isNotEmpty() == true
            }

            Log.d(TAG, "Found non-empty directories: ${directories?.map { it.name }}")

            val allCollections = directories?.mapNotNull { dir ->
                val name = dir.name
                val imgCount = File(dir, "image").listFiles()?.size ?: 0
                val metadataFile = File(dir, "metadata.json")

                if (!metadataFile.exists()) return@mapNotNull null
                val metadataJson = JSONObject(metadataFile.readText())

                val location = metadataJson.optString("locationString", "Unknown")
                val lat = metadataJson.optDouble("lat", 0.0)
                val lon = metadataJson.optDouble("lon", 0.0)
                val timestamp = metadataJson.optLong("timestamp", 0L)
                val detections = metadataJson.optInt("detections", 0)
                val owner = metadataJson.optString("owner", "Unknown")

                Collection(name, imgCount, detections, timestamp, location, lat, lon, owner)
            } ?: emptyList()

            // 🔍 Filter only collections owned by current user
            return@withContext allCollections.filter { it.owner == currentUser }

        } else {
            Log.d(TAG, "Collections directory does not exist.")
            return@withContext emptyList()
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
                val imgCount = File(collectionDir, "imgCount").listFiles()?.size ?: 0
                val detections = metadataJson.optInt("detections", 0)
                val timestamp = metadataJson.optLong("timestamp", 0L)
                val owner = metadataJson.optString("owner", "Unknown")

                return@withContext Collection(name, imgCount,detections, timestamp, location, lat, lon, owner)
            } else {
                Log.d(TAG, "Collection $collectionName or its metadata.json does not exist.")
            }
        }
        return@withContext null
    }

    suspend fun updateCollectionMetadata(
        collectionName: String,
        newImgCount: Int? = null,
        newDetections: Int? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val collectionsDir = checkForCollectionsDir()
        if (collectionsDir != null && collectionsDir.exists()) {
            val collectionDir = File(collectionsDir, collectionName)
            val metadataFile = File(collectionDir, "metadata.json")

            if (collectionDir.exists() && metadataFile.exists()) {
                val metadataJson = JSONObject(metadataFile.readText())

                newImgCount?.let { metadataJson.put("imgCount", it) }
                newDetections?.let { metadataJson.put("detections", it) }

                metadataFile.writeText(metadataJson.toString())
                Log.d(TAG, "Collection $collectionName metadata updated.")
                return@withContext true
            } else {
                Log.e(TAG, "Collection $collectionName or metadata.json does not exist.")
            }
        }
        return@withContext false
    }

    // Create a new collection directory along with 'image' and 'result' subdirectories
    fun createCollectionDir(name: String, timestamp: Long, locationString: String="none", lat: Double=0.0, lon: Double=0.0, owner: String): Boolean {
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
                        put("detections",0)
                        put("timestamp", timestamp)
                        put("owner", owner)
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

        var spandukCount = 0

        for (i in 0 until detections.length()) {
            val detection = detections.getJSONObject(i)
            val className = detection.getString("class_name")

            when (className) {
                "spanduk" -> spandukCount++
            }
        }

        return ClassOccurence(
            spanduk = spandukCount
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

    suspend fun reverseGeocodeLocation(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses: List<Address> = geocoder.getFromLocation(lat, lon, 1) ?: return@withContext "Unknown Location"

            if (addresses.isNotEmpty()) {
                val address = addresses[0]
                buildString {
                    if (!address.featureName.isNullOrEmpty()) append(address.featureName + ", ")
                    if (!address.thoroughfare.isNullOrEmpty()) append(address.thoroughfare + ", ")
                    if (!address.subThoroughfare.isNullOrEmpty()) append(address.subThoroughfare + ", ")
                    if (!address.subLocality.isNullOrEmpty()) append(address.subLocality + ", ")
                    if (!address.locality.isNullOrEmpty()) append(address.locality + ", ")
                    if (!address.subAdminArea.isNullOrEmpty()) append(address.subAdminArea + ", ")
                    if (!address.adminArea.isNullOrEmpty()) append(address.adminArea + ", ")
                    if (!address.postalCode.isNullOrEmpty()) append(address.postalCode)
                }.trim().removeSuffix(",")
            } else {
                "Unknown Location"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocoding failed: ${e.message}", e)
            "Unknown Location"
        }
    }


}
