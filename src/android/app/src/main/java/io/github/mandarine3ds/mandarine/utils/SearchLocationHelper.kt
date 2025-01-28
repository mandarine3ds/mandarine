// Copyright 2025 Mandarine Project

package io.github.mandarine3ds.mandarine.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.mandarine3ds.mandarine.MandarineApplication
import io.github.mandarine3ds.mandarine.utils.GameHelper
import androidx.preference.PreferenceManager

interface SearchLocationHelper {
    companion object {

        /**
         * Returns the list of selected search locations.
         * @return A list of URIs of selected search locations
         */
        fun getSearchLocations(context: Context): List<Uri> {
            val preferences = PreferenceManager.getDefaultSharedPreferences(MandarineApplication.appContext)
            val locations = preferences.getString(GameHelper.KEY_GAME_PATH, "").orEmpty().split("|")
            val urisList = mutableListOf<Uri>()

            locations.forEach {
                if (it.isNotEmpty()) {
                    urisList.add(Uri.parse(it))
                }
            }
            return urisList
        }

        /**
         * Adds the given search location to the emulation settings
         * @param uri The URI of the selected search location
         * @return The exit status of the installation process
         */
        fun addLocation(context: Context, uri: Uri): SearchLocationResult {
            val locations = getSearchLocations(context)
            val preferences = PreferenceManager.getDefaultSharedPreferences(MandarineApplication.appContext)

            if (locations.contains(uri)) {
                return SearchLocationResult.AlreadyAdded
            } else {
                val newLocations = buildString {
                    append(locations.joinToString(separator = "|") { it.toString() })
                    if (locations.isNotEmpty()) append("|")
                    append(uri.toString())
                }

                preferences.edit()
                    .putString(GameHelper.KEY_GAME_PATH, newLocations)
                    .apply()
            }
            return SearchLocationResult.Success
        }

        /**
         * Deletes the given URI from the emulation settings
         * @param uri The URI to remove from the list
         * @return The exit status of the removal process
         */
        fun deleteLocation(context: Context, uri: Uri): SearchLocationResult {
            val locations = getSearchLocations(context)
            val preferences = PreferenceManager.getDefaultSharedPreferences(MandarineApplication.appContext)

            val newValue = locations.filterNot { it.toString() == uri.toString() }
                .joinToString(separator = "|") { it.toString() }

            preferences.edit()
                .putString(GameHelper.KEY_GAME_PATH, newValue)
                .apply()

            return SearchLocationResult.Deleted
        }
    }
}

enum class SearchLocationResult {
    Success,
    AlreadyAdded,
    Deleted
}
