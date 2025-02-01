// Copyright 2025 Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.


package io.github.mandarine3ds.mandarine.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.OutputStream

object DriversFetcher {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }

    @Serializable
    data class GitHubRelease(
        val name: String,
        val assets: List<Asset> = emptyList()
    )

    @Serializable
    data class Asset(val browser_download_url: String)

    suspend fun fetchReleases(repoUrl: String): List<Pair<String, String?>> {
        val repoPath = repoUrl.removePrefix("https://github.com/")
        val validationUrl = "https://api.github.com/repos/$repoPath/contents/.adrenoDrivers"
        val apiUrl = "https://api.github.com/repos/$repoPath/releases"

        return try {
            val isValid = withContext(Dispatchers.IO) {
                try {
                    httpClient.get(validationUrl).status.value == 200
                } catch (e: Exception) {
                    false
                }
            }

            if (!isValid) {
                Log.d("DriversFetcher", "Provided driver repo url is not valid.")
                return emptyList()
            }

            val releases: List<GitHubRelease> = withContext(Dispatchers.IO) {
                httpClient.get(apiUrl).body()
            }
            releases.map { release ->
                val assetUrl = release.assets.firstOrNull()?.browser_download_url
                release.name to assetUrl
            }
        } catch (e: Exception) {
            Log.e("DriversFetcher", "Error fetching releases: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun downloadAsset(context: Context, assetUrl: String, destinationUri: Uri): DownloadResult {
        return try {
            withContext(Dispatchers.IO) {
                val response: HttpResponse = httpClient.get(assetUrl)
                context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    writeResponseToStream(response, outputStream)
                } ?: return@withContext DownloadResult.Error("Failed to open ${destinationUri.toString()}")
            }
            DownloadResult.Success
        } catch (e: Exception) {
            Log.e("DriversFetcher", "Error downloading file: ${e.message}", e)
            DownloadResult.Error(e.message)
        }
    }

    private suspend fun writeResponseToStream(response: HttpResponse, outputStream: OutputStream) {
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(8192) // 8KB buffer size

        while (!channel.isClosedForRead) {
            val bytesRead = channel.readAvailable(buffer)
            if (bytesRead > 0) {
                outputStream.write(buffer, 0, bytesRead)
            }
        }
        outputStream.flush()
    }

    sealed class DownloadResult {
        object Success : DownloadResult()
        data class Error(val message: String?) : DownloadResult()
    }
}
