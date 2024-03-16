package be.kuleuven.privacybuddy.data

import java.util.Date

data class LocationData(
    val timestamp: String,
    val date: Date,
    val appName: String,
    val usageType: String,
    val interactionType: String
)