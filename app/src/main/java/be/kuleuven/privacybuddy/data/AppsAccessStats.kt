package be.kuleuven.privacybuddy.data

data class AppAccessStats(
    val appName: String,

    val totalAccesses: Int,

    val frequencyPerDay: Float,

    val approximateAccesses: Int,
    val preciseAccesses: Int,

    val foregroundAccesses: Int,
    val backgroundAccesses: Int,
    val subliminalAccesses: Int,

    val preciseForegroundAccesses: Int,
    val approximateForegroundAccesses: Int,

    val preciseBackgroundAccesses: Int,
    val approximateBackgroundAccesses: Int,

    val preciseSubliminalAccesses: Int,
    val approximateSubliminalAccesses: Int

)
