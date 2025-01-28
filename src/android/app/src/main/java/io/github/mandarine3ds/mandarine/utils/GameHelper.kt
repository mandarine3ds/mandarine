// Copyright 2025 Citra / Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.utils

import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import io.github.mandarine3ds.mandarine.utils.SearchLocationHelper
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.github.mandarine3ds.mandarine.MandarineApplication
import io.github.mandarine3ds.mandarine.NativeLibrary
import io.github.mandarine3ds.mandarine.model.CheapDocument
import io.github.mandarine3ds.mandarine.model.Game
import io.github.mandarine3ds.mandarine.model.GameInfo
import java.io.IOException

object GameHelper {
    const val KEY_GAME_PATH = "game_path"
    const val KEY_GAMES = "Games"

    private lateinit var preferences: SharedPreferences

    fun getGames(): List<Game> {
        val games = mutableListOf<Game>()
        val context = MandarineApplication.appContext
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val gamesDirs = SearchLocationHelper.getSearchLocations(context)

        gamesDirs.forEach { searchLocation ->
            addGamesRecursive(games, FileUtil.listFiles(searchLocation), 3)
            NativeLibrary.getInstalledGamePaths().forEach {
                val game = getGame(Uri.parse(it), isInstalled = true, addedToLibrary = true)
                if (!games.containsGame(game)) {
                    games.add(game)
                }
            }
        }

        // Cache list of games found on disk
        val serializedGames = mutableSetOf<String>()
        games.forEach {
            serializedGames.add(Json.encodeToString(it))
        }
        preferences.edit()
            .remove(KEY_GAMES)
            .putStringSet(KEY_GAMES, serializedGames)
            .apply()

        return games.toList()
    }

    private fun addGamesRecursive(
        games: MutableList<Game>,
        files: Array<CheapDocument>,
        depth: Int
    ) {
        if (depth <= 0) {
            return
        }

        files.forEach {
            if (it.isDirectory) {
                addGamesRecursive(games, FileUtil.listFiles(it.uri), depth - 1)
            } else {
                if (Game.allExtensions.contains(FileUtil.getExtension(it.uri))) {
                    val game = getGame(it.uri, isInstalled = false, addedToLibrary = true)
                    if (!games.containsGame(game)) {
                        games.add(game)
                    }
                }
            }
        }
    }

    fun getGame(uri: Uri, isInstalled: Boolean, addedToLibrary: Boolean): Game {
        val filePath = uri.toString()
        val gameInfo: GameInfo? = try {
            GameInfo(filePath)
        } catch (e: IOException) {
            null
        }

        val newGame = Game(
            (gameInfo?.getTitle() ?: FileUtil.getFilename(uri)).replace("[\\t\\n\\r]+".toRegex(), " "),
            filePath.replace("\n", " "),
            filePath,
            NativeLibrary.getTitleId(filePath),
            gameInfo?.getCompany() ?: "",
            gameInfo?.getRegions() ?: "Invalid region",
            isInstalled,
            NativeLibrary.getIsSystemTitle(filePath),
            gameInfo?.getIsVisibleSystemTitle() ?: false,
            gameInfo?.getIcon(),
            if (FileUtil.isNativePath(filePath)) {
                MandarineApplication.documentsTree.getFilename(filePath)
            } else {
                FileUtil.getFilename(Uri.parse(filePath))
            }
        )

        if (addedToLibrary) {
            val addedTime = preferences.getLong(newGame.keyAddedToLibraryTime, 0L)
            if (addedTime == 0L) {
                preferences.edit()
                    .putLong(newGame.keyAddedToLibraryTime, System.currentTimeMillis())
                    .apply()
            }
        }

        return newGame
    }

    private fun MutableList<Game>.containsGame(game: Game): Boolean {
        return this.any { it.path == game.path }
    }
}
