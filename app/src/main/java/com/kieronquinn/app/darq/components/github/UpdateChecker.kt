package com.kieronquinn.app.darq.components.github

import android.content.Context
import android.os.Parcelable
import android.util.Log
import com.kieronquinn.app.darq.BuildConfig
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.File

class UpdateChecker {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    companion object {
        private const val GITHUB_REPO = "Arora-Sir/DarQ-Reborn"
        private const val BASE_URL = "https://api.github.com/repos/$GITHUB_REPO/"
        private const val RELEASES_URL = "https://github.com/$GITHUB_REPO/releases"
    }

    private fun isNewerVersion(remoteTag: String, localTag: String): Boolean {
        val remote = remoteTag.replace("v", "").split(".").mapNotNull { it.toIntOrNull() }
        val local = localTag.replace("v", "").split(".").mapNotNull { it.toIntOrNull() }
        val length = maxOf(remote.size, local.size)
        for (i in 0 until length) {
            val r = remote.getOrElse(i) { 0 }
            val l = local.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    fun getLatestRelease() = callbackFlow {
        Log.d("DarQUpdate", "getLatestRelease: Checking for update started")
        withContext(Dispatchers.IO){
            val release = getLatestReleaseResponse()
            Log.d("DarQUpdate", "getLatestRelease: Fetched release response: $release")
            release?.let { gitHubReleaseResponse ->
                val currentTag = gitHubReleaseResponse.tagName
                Log.d("DarQUpdate", "getLatestRelease: Current remote tag = $currentTag, Local tag = ${BuildConfig.TAG_NAME}")
                if (currentTag != null) {
                    val remoteNormalized = currentTag.replace("v", "").trim()
                    val localNormalized = BuildConfig.TAG_NAME.replace("v", "").trim()
                    Log.d("DarQUpdate", "getLatestRelease: Normalized compare: remote=$remoteNormalized, local=$localNormalized")
                    if (remoteNormalized != localNormalized) {
                        Log.d("DarQUpdate", "getLatestRelease: Remote tag differs from local tag, looking for apk asset")
                        val assets = gitHubReleaseResponse.assets
                        Log.d("DarQUpdate", "getLatestRelease: Total assets found: ${assets?.size ?: 0}")
                        assets?.forEach {
                            Log.d("DarQUpdate", "getLatestRelease: Found asset option: name=${it.name}, url=${it.browserDownloadUrl}")
                        }
                        val asset =
                            gitHubReleaseResponse.assets?.firstOrNull { it.name?.endsWith(".apk") == true }
                        if (asset == null) {
                            Log.e("DarQUpdate", "getLatestRelease: No asset ending in .apk found")
                            this@callbackFlow.trySend(null).isSuccess
                            return@let
                        }
                        val releaseUrl =
                            asset.browserDownloadUrl?.replace("/download/", "/tag/")?.let {
                                it.substring(0, it.lastIndexOf("/"))
                            }
                        val name = gitHubReleaseResponse.name ?: run {
                            Log.e("DarQUpdate", "getLatestRelease: Release name is null")
                            this@callbackFlow.trySend(null).isSuccess
                            return@let
                        }
                        val body = gitHubReleaseResponse.body ?: run {
                            Log.e("DarQUpdate", "getLatestRelease: Release body is null")
                            this@callbackFlow.trySend(null).isSuccess
                            return@let
                        }
                        val publishedAt = gitHubReleaseResponse.publishedAt ?: run {
                            Log.e("DarQUpdate", "getLatestRelease: Release publishedAt is null")
                            this@callbackFlow.trySend(null).isSuccess
                            return@let
                        }
                        // Construct a unique filename for the version (e.g. DarQ_2.2.9.apk)
                        val uniqueAssetName = "DarQ_${currentTag}.apk"
                        val updateObj = Update(
                            name,
                            body,
                            publishedAt,
                            asset.browserDownloadUrl ?: RELEASES_URL,
                            uniqueAssetName,
                            releaseUrl ?: RELEASES_URL
                        )
                        Log.d("DarQUpdate", "getLatestRelease: Emitting update: $updateObj")
                        this@callbackFlow.trySend(updateObj).isSuccess
                    } else {
                        Log.d("DarQUpdate", "getLatestRelease: Remote tag is the same as local tag. No update needed.")
                        this@callbackFlow.trySend(null).isSuccess
                    }
                } else {
                    Log.e("DarQUpdate", "getLatestRelease: Remote tag name is null")
                    this@callbackFlow.trySend(null).isSuccess
                }
            } ?: run {
                Log.e("DarQUpdate", "getLatestRelease: Release response is null")
                this@callbackFlow.trySend(null).isSuccess
            }
        }
        awaitClose { Log.d("DarQUpdate", "getLatestRelease: callbackFlow closed") }
    }

    fun deleteStaleCache(context: Context, currentAssetName: String) {
        val folder = File(context.cacheDir, "updates")
        Log.d("DarQUpdate", "deleteStaleCache: cache folder path = ${folder.absolutePath}, keeping currentAssetName = $currentAssetName")
        folder.listFiles()?.forEach { file ->
            if (file.name != currentAssetName) {
                val deleted = file.delete()
                Log.d("DarQUpdate", "deleteStaleCache: Deleted stale file ${file.name}: $deleted")
            } else {
                Log.d("DarQUpdate", "deleteStaleCache: Keeping active file ${file.name}")
            }
        }
    }

    private fun getLatestReleaseResponse(): GitHubReleaseResponse? {
        val service: GitHubService = retrofit.create(GitHubService::class.java)
        Log.d("DarQUpdate", "getLatestReleaseResponse: Making GitHub API call to $BASE_URL")
        runCatching {
            service.getLatestRelease().execute().body()
        }.onSuccess {
            Log.d("DarQUpdate", "getLatestReleaseResponse: API call success, body: $it")
            return it
        }.onFailure {
            Log.e("DarQUpdate", "getLatestReleaseResponse: API call failed", it)
            return null
        }
        return null
    }

    interface GitHubService {
        @GET("releases/latest")
        fun getLatestRelease(): Call<GitHubReleaseResponse>
    }

    @Parcelize
    data class Update(val name: String, val changelog: String, val timestamp: String, val assetUrl: String, val assetName: String, val releaseUrl: String): Parcelable

}