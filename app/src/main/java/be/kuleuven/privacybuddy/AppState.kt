package be.kuleuven.privacybuddy

import be.kuleuven.privacybuddy.data.AppAccessInfo

object AppState {
    var selectedGeoJsonFile: String = "dummy_location_3.geojson"
    var topAccessedAppsCache: List<AppAccessInfo>? = null

    fun clearTopAccessedAppsCache() {
        topAccessedAppsCache = null
    }

}
