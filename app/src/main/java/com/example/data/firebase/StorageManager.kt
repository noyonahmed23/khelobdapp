package com.example.data.firebase

// NOTE: Firebase Storage dependency must be added to app/build.gradle.kts:
//   implementation(libs.firebase.storage)
// And in libs.versions.toml:
//   firebase-storage = { group = "com.google.firebase", name = "firebase-storage" }

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object StorageManager {
    private const val TAG = "StorageManager"

    val storage: FirebaseStorage? by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase Storage", e)
            null
        }
    }

    /**
     * Upload an image from a Uri to Firebase Storage.
     * Returns the download URL string, or null on failure.
     */
    suspend fun uploadImage(
        uri: Uri,
        path: String, // e.g. "profile_pictures/uid123.jpg"
        onProgress: ((Float) -> Unit)? = null
    ): String? {
        val storageRef = storage?.reference?.child(path) ?: return null
        return try {
            val uploadTask = storageRef.putFile(uri)
            // Optional progress tracking
            onProgress?.let { callback ->
                uploadTask.addOnProgressListener { snapshot ->
                    val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat()
                    callback(progress)
                }
            }
            uploadTask.await()
            val downloadUrl = storageRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Image upload failed for path $path", e)
            throw e
        }
    }

    fun getProfilePicturePath(userId: String) = "profile_pictures/$userId.jpg"
    fun getProfileBannerPath(userId: String) = "profile_banners/$userId.jpg"
    fun getTeamProfileImagePath(teamId: String) = "team_images/$teamId.jpg"
    fun getTeamBannerPath(teamId: String) = "team_banners/$teamId.jpg"
    fun getChallengePicPath(challengeId: String, userId: String) = "challenge_proofs/${challengeId}_${userId}.jpg"
    fun getWelcomePopupPath() = "welcome_popup/banner.jpg"
}