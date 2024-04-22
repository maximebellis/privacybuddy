package be.kuleuven.privacybuddy.data

data class AppAccessStats(
    val appName: String,

    val totalAccesses: Int,

    val days: Int,

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
    val approximateSubliminalAccesses: Int,

    var privacyScore: Double,
    val numberOfPOIs: Int

)
