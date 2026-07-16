package com.kieronquinn.app.darq.ui.screens.bottomsheets.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.darq.BuildConfig
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.components.github.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

abstract class UpdateDownloadBottomSheetViewModel : ViewModel() {

    abstract val downloadState: Flow<State>

    abstract fun startDownload(context: Context, update: UpdateChecker.Update)
    abstract fun cancelDownload(context: Context)
    abstract fun openPackageInstaller(context: Context, uri: Uri)

    sealed class State {
        object Idle : State()
        data class Downloading(val progress: Int) : State()
        data class Done(val fileUri: Uri) : State()
        object Failed : State()
    }
}

class UpdateDownloadBottomSheetViewModelImpl : UpdateDownloadBottomSheetViewModel() {

    companion object {
        private const val NOTIFICATION_ID = 9001
        private const val CHANNEL_ID = "darq_update_download"
    }

    private val okHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private var _downloadState = MutableStateFlow<State>(State.Idle)
    override val downloadState = _downloadState.asStateFlow()

    // ── Notification helpers ──────────────────────────────────────────────

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.app_name) + " Updates",
                    NotificationManager.IMPORTANCE_LOW
                )
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun showProgressNotification(context: Context, progress: Int) {
        ensureNotificationChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Downloading update… $progress%")
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun showSizeProgressNotification(context: Context, downloadedBytes: Long) {
        ensureNotificationChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sizeMB = downloadedBytes / (1024.0 * 1024.0)
        val formattedSize = String.format("%.2f MB", sizeMB)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Downloading update… ($formattedSize)")
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(100, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletedNotification(context: Context, uri: Uri) {
        ensureNotificationChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "update_downloads_complete",
                "Update Complete",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val installIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, installIntent, pendingIntentFlags)

        val notification = NotificationCompat.Builder(context, "update_downloads_complete")
            .setContentTitle("Update Downloaded")
            .setContentText("Tap to install the update")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.cancel(NOTIFICATION_ID)
        nm.notify(1001, notification)
    }

    private fun cancelNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }

    // ── Download logic ────────────────────────────────────────────────────

    override fun startDownload(context: Context, update: UpdateChecker.Update) {
        _downloadState.value = State.Idle
        val downloadFolder = File(context.cacheDir, "updates")
        val existingFile = File(downloadFolder, update.assetName)
        if (existingFile.exists() && existingFile.length() > 0) {
            viewModelScope.launch {
                val outputUri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    existingFile
                )
                showCompletedNotification(context, outputUri)
                _downloadState.emit(State.Done(outputUri))
            }
        } else {
            downloadUpdate(context, update.assetUrl, update.assetName)
        }
    }

    private fun downloadUpdate(context: Context, url: String, fileName: String) {
        viewModelScope.launch {
            _downloadState.emit(State.Downloading(0))
            showProgressNotification(context, 0)

            withContext(Dispatchers.IO) {
                val downloadFolder = File(context.cacheDir, "updates").also { it.mkdirs() }
                val outputFile = File(downloadFolder, fileName)
                try {
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }

                    val request = Request.Builder().url(url).build()
                    val response = okHttpClient.newCall(request).execute()

                    if (!response.isSuccessful) {
                        if (outputFile.exists()) outputFile.delete()
                        cancelNotification(context)
                        _downloadState.emit(State.Failed)
                        return@withContext
                    }

                    val body = response.body() ?: run {
                        if (outputFile.exists()) outputFile.delete()
                        cancelNotification(context)
                        _downloadState.emit(State.Failed)
                        return@withContext
                    }

                    val totalBytes = body.contentLength()
                    var downloadedBytes = 0L
                    var lastNotifiedProgress = -1
                    var lastNotifiedBytes = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(outputFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                ensureActive()
                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead
                                if (totalBytes > 0) {
                                    val progress = (downloadedBytes * 100 / totalBytes).toInt()
                                    _downloadState.emit(State.Downloading(progress))
                                    if (progress >= lastNotifiedProgress + 5) {
                                        lastNotifiedProgress = progress
                                        showProgressNotification(context, progress)
                                    }
                                } else {
                                    _downloadState.emit(State.Downloading(0))
                                    if (downloadedBytes >= lastNotifiedBytes + 500 * 1024) {
                                        lastNotifiedBytes = downloadedBytes
                                        showSizeProgressNotification(context, downloadedBytes)
                                    }
                                }
                            }
                        }
                    }

                    val outputUri = FileProvider.getUriForFile(
                        context,
                        BuildConfig.APPLICATION_ID + ".provider",
                        outputFile
                    )
                    showCompletedNotification(context, outputUri)
                    _downloadState.emit(State.Done(outputUri))

                } catch (e: Exception) {
                    try {
                        if (outputFile.exists()) {
                            outputFile.delete()
                        }
                    } catch (ex: Exception) {}
                    cancelNotification(context)
                    _downloadState.emit(State.Failed)
                }
            }
        }
    }

    override fun openPackageInstaller(context: Context, uri: Uri) {
        Intent(Intent.ACTION_VIEW, uri).apply {
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.also {
            context.startActivity(it)
        }
    }

    override fun cancelDownload(context: Context) {
        cancelNotification(context)
        viewModelScope.launch {
            _downloadState.emit(State.Idle)
        }
    }
}

