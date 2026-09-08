package com.tomcat927.bemfacontrol.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.tomcat927.bemfacontrol.data.model.GitHubRelease
import com.tomcat927.bemfacontrol.data.model.ReleaseInfo
import com.tomcat927.bemfacontrol.data.network.GitHubApi
import com.tomcat927.bemfacontrol.diagnostics.RuntimeLog
import java.io.File
import java.security.MessageDigest

class UpdateRepository(
    private val context: Context,
    private val api: GitHubApi,
) {

    suspend fun checkForUpdate(currentVersionCode: Int): ReleaseInfo? {
        val startedAt = System.currentTimeMillis()
        try {
            val release = api.latestRelease()
            val tagName = release.tagName.removePrefix("v")
            val apkAsset = release.assets.find { it.name.endsWith(".apk") }
            val shaAsset = release.assets.find { it.name.endsWith(".sha256") }
            if (apkAsset == null) {
                RuntimeLog.debug("update: no APK asset in latest release")
                return null
            }

            // Parse versionCode from tag: v2026.09.08.2020 -> hash of digits
            val tagVersionCode = parseVersionCode(tagName)
            val isNewer = tagVersionCode > currentVersionCode

            val githubUrl = apkAsset.browserDownloadUrl
            val proxyUrl = "https://gh-proxy.com/$githubUrl"
            val sha256Url = shaAsset?.browserDownloadUrl ?: ""

            RuntimeLog.debug("update: latest=$tagName current=$currentVersionCode isNewer=$isNewer in ${System.currentTimeMillis() - startedAt}ms")

            return ReleaseInfo(
                versionName = tagName,
                downloadUrl = githubUrl,
                proxyUrl = proxyUrl,
                sha256Url = sha256Url,
                releaseNotes = release.body,
                isNewer = isNewer,
            )
        } catch (throwable: Throwable) {
            RuntimeLog.error("update check failed", throwable)
            return null
        }
    }

    suspend fun downloadAndInstall(
        releaseInfo: ReleaseInfo,
        useProxy: Boolean,
        onProgress: (Float) -> Unit,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val url = if (useProxy) releaseInfo.proxyUrl else releaseInfo.downloadUrl
            RuntimeLog.debug("update: downloading from $useProxy")

        val cacheDir = File(context.externalCacheDir, "apk_updates").apply { mkdirs() }
        val apkFile = File(cacheDir, "bemfa-control-update.apk")

        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val request = okhttp3.Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                RuntimeLog.error("update: download failed HTTP ${response.code}")
                return@withContext false
            }

            response.body?.byteStream()?.use { input ->
                apkFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            // Verify SHA-256 if available
            if (releaseInfo.sha256Url.isNotEmpty()) {
                val shaClient = okhttp3.OkHttpClient.Builder().build()
                val shaRequest = okhttp3.Request.Builder().url(releaseInfo.sha256Url).build()
                val shaResponse = shaClient.newCall(shaRequest).execute()
                val expectedSha = shaResponse.body?.string()?.trim()?.split(Regex("\\s+"))?.firstOrNull()
                if (expectedSha != null) {
                    val actualSha = sha256(apkFile)
                    if (!expectedSha.equals(actualSha, ignoreCase = true)) {
                        RuntimeLog.error("update: SHA-256 mismatch: expected=$expectedSha actual=$actualSha")
                        apkFile.delete()
                        return@withContext false
                    }
                    RuntimeLog.debug("update: SHA-256 verified")
                }
            }

            // Install
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            RuntimeLog.debug("update: install intent sent")
            return@withContext true
        } catch (throwable: Throwable) {
            RuntimeLog.error("update: download/install failed", throwable)
            return@withContext false
        }
        }
    }

    private fun parseVersionCode(tagName: String): Int {
        // Use BuildConfig.versionCode, but for comparison use a simple hash
        // Since tag format is like 2026.09.08.2020, we can't use it as versionCode directly
        // We'll compare by parsing and computing a comparable int
        val parts = tagName.split(".")
        if (parts.size >= 3) {
            val year = parts[0].takeLast(2).toIntOrNull() ?: 0
            val month = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val day = parts.getOrNull(2)?.toIntOrNull() ?: 0
            val time = parts.getOrNull(3)?.toIntOrNull() ?: 0
            return year * 1000000 + month * 10000 + day * 100 + (time % 100)
        }
        return 0
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
