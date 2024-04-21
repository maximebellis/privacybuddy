package be.kuleuven.privacybuddy

import be.kuleuven.privacybuddy.data.AppAccessStats

object AppState {
    var selectedGeoJsonFile: String = "dummy_location_3.geojson"
    var topAccessedAppsCache: List<AppAccessStats>? = null  // Correctly type the cache

    fun clearTopAccessedAppsCache() {
        topAccessedAppsCache = null
    }
}

