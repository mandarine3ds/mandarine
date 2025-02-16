// Copyright 2025 Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.utils

import androidx.documentfile.provider.DocumentFile
import io.github.mandarine3ds.mandarine.model.Game
import io.github.mandarine3ds.mandarine.MandarineApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

@Serializable
data class PlayTimeData(
    val titleId: Long,
    val title: String,
    var totalPlayTimeMs: Long = 0
)

object PlayTimeTracker {
    private const val PLAYTIME_FILENAME = "playtime.json"
    private val playTimes = mutableMapOf<Long, PlayTimeData>()

    init {
        loadPlayTimes()
    }

    fun addPlayTime(game: Game, sessionTimeMs: Long) {
        val data = playTimes.getOrPut(game.titleId) {
            PlayTimeData(game.titleId, game.title)
        }
        data.totalPlayTimeMs += sessionTimeMs
        savePlayTimes()
    }

    fun getPlayTime(titleId: Long): String {
        // Reload playtime in case of manual file editing
        loadPlayTimes()

        val totalSeconds = (playTimes[titleId]?.totalPlayTimeMs ?: 0) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    private fun loadPlayTimes() {
        try {
            val root = DocumentFile.fromTreeUri(MandarineApplication.appContext, PermissionsHandler.mandarineDirectory)
            val logDir = root?.findFile("log") ?: return
            val playTimeFile = logDir.findFile(PLAYTIME_FILENAME) ?: return

            val context = MandarineApplication.appContext
            val inputStream = context.contentResolver.openInputStream(playTimeFile.uri) ?: return
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()

            val loadedData = Json.decodeFromString<List<PlayTimeData>>(jsonString)
            playTimes.clear()
            loadedData.forEach { playTimes[it.titleId] = it }

            reader.close()
        } catch (e: Exception) {
            Log.error("Failed to load play times: ${e.message}")
        }
    }

    private fun savePlayTimes() {
        try {
            val root = DocumentFile.fromTreeUri(MandarineApplication.appContext, PermissionsHandler.mandarineDirectory)
            val logDir = root?.findFile("log")
                ?: root?.createDirectory("log")
                ?: return

            var playTimeFile = logDir.findFile(PLAYTIME_FILENAME)
            if (playTimeFile == null) {
                playTimeFile = logDir.createFile("application/json", PLAYTIME_FILENAME) ?: return
            }

            val jsonString = Json.encodeToString(playTimes.values.toList())
            val outputStream = MandarineApplication.appContext.contentResolver.openOutputStream(playTimeFile.uri) ?: return
            outputStream.write(jsonString.toByteArray())
            outputStream.close()
        } catch (e: Exception) {
            Log.error("Failed to save play times: ${e.message}")
        }
    }

    fun deletePlayTime(titleId: Long) {
        playTimes.remove(titleId)
        savePlayTimes()
    }
}