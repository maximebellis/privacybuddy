package be.kuleuven.privacybuddy

import be.kuleuven.privacybuddy.data.AppAccessStats
import be.kuleuven.privacybuddy.data.LocationData

object AppState {
    var selectedGeoJsonFile: String = "dummy_location_3.geojson"
    var topAccessedAppsCache: List<AppAccessStats>? = null  // Correctly type the cache
    var globalData: List<LocationData> = emptyList()

    var selectedUsageTypes: MutableList<String> = mutableListOf()
    var selectedInteractionTypes: MutableList<String> = mutableListOf()


    fun clearTopAccessedAppsCache() {
        topAccessedAppsCache = null
    }
}

