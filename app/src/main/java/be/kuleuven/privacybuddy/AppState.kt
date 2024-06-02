package be.kuleuven.privacybuddy

import be.kuleuven.privacybuddy.data.AppAccessStats
import be.kuleuven.privacybuddy.data.LocationData

object AppState {
    var selectedGeoJsonFile: String = "data.geojson"
    var topAccessedAppsCache: List<AppAccessStats>? = null
    var globalData: List<LocationData> = emptyList()

    var selectedUsageTypes: MutableList<String> = mutableListOf()
    var selectedInteractionTypes: MutableList<String> = mutableListOf()


    fun clearTopAccessedAppsCache() {
        topAccessedAppsCache = null
    }
}

