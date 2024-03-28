package be.kuleuven.privacybuddy.data

import java.util.Date

data class LocationData(
    val timestamp: String,
    val date: Date,
    val appName: String,
    val usageType: String,
    val interactionType: String,
    val accuracy: Double? = null,
    val speed: Double? = null,
    val bearing: Double? = null,
    val screenState: String? = null,
    // Add coordinates
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null
)

