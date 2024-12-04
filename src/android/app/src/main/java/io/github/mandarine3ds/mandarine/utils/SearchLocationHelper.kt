
package io.github.mandarine3ds.mandarine.utils

import android.content.Context
import android.net.Uri
import android.util.Log

interface SearchLocationHelper {
    companion object {

        /**
         * Returns the list of selected search locations.
         * @return A list of URIs of selected search locations
         */
        fun getSearchLocations(context: Context): List<Uri> {
            val locations = "" // TODO: Implement locations obtaining logic
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
         * @param uri The uri os the selected search location
         * @return The exit status of the installation process
         */
        fun addLocation(context : Context, uri : Uri) : SearchLocationResult {
            var locations = getSearchLocations(context)
            if(locations.contains(uri)){
                return SearchLocationResult.AlreadyAdded
            }
            else{
                // TODO: Implement saving logic
            }
            return SearchLocationResult.Success
        }

        /**
         * Deletes the given uri from the emulation settings
         * @param uri The input stream to read the driver from
         * @return The exit status of the installation process
         */
        fun deleteLocation(context : Context, uri : Uri) : SearchLocationResult {
            var locations = ""

            var newValue = ""
            for (location in locations) {
                if(location != uri.toString() && location != "")
                    newValue = StringBuilder().append(newValue).append(if(newValue.isEmpty()) "" else "|").append(location).toString()
            }
            return SearchLocationResult.Deleted
        }
    }
}

enum class SearchLocationResult {
    Success,
    AlreadyAdded,
    Deleted
}
