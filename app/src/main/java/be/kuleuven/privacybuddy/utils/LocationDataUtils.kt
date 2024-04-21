package be.kuleuven.privacybuddy.utils

import android.content.Context
import android.util.Log
import be.kuleuven.privacybuddy.AppState
import be.kuleuven.privacybuddy.BaseActivity
import be.kuleuven.privacybuddy.adapter.TimelineItem
import be.kuleuven.privacybuddy.data.AppAccessStats
import be.kuleuven.privacybuddy.data.LocationData
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.mapbox.geojson.Point



object LocationDataUtils {

    fun prepareTimelineItems(events: List<LocationData>): List<TimelineItem> {
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val sortedEvents = events.sortedByDescending { it.date }
        return sortedEvents
            .groupBy { dateFormatter.format(it.date) }
            .flatMap { (dateString, events) ->
                listOf(TimelineItem.DateLabel(dateFormatter.parse(dateString)!!)) +
                        events.map { TimelineItem.EventItem(it) }
            }
    }
    fun loadGeoJsonFromAssets(selectedAppName: String?, context: Context, filename: String = AppState.selectedGeoJsonFile, days: Int): List<LocationData> {
        return try {
            val featureCollection = parseGeoJsonFromAssets(context, filename)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            val calendar = Calendar.getInstance().apply {
                add(Calendar.DATE, -days)
            }
            val cutoffDate = calendar.time

            featureCollection.features()?.mapNotNull { feature ->
                val timestamp = feature.getStringProperty("timestamp")
                val date = dateFormat.parse(timestamp)

                if (date != null && date.after(cutoffDate) && (selectedAppName == null || feature.getStringProperty("appName") == selectedAppName)) {
                    // Check if the feature's geometry is a Point
                    val point = feature.geometry() as? com.mapbox.geojson.Point
                    val coordinates = point?.coordinates()

                    LocationData(
                        timestamp = timestamp,
                        date = date,
                        appName = feature.getStringProperty("appName"),
                        usageType = feature.getStringProperty("usageType"),
                        interactionType = feature.getStringProperty("interactionType"),
                        accuracy = feature.getNumberProperty("accuracy")?.toDouble(),
                        speed = feature.getNumberProperty("speed")?.toDouble(),
                        bearing = feature.getNumberProperty("bearing")?.toDouble(),
                        screenState = feature.getStringProperty("screenState"),
                        latitude = point?.latitude(),
                        longitude = point?.longitude(),
                        altitude = if (coordinates?.size == 3) coordinates[2] else null
                    )
                } else {
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("LocationDataUtils", "Error loading or parsing GeoJSON data", e)
            emptyList()
        }
    }


    private fun parseGeoJsonFromAssets(context: Context, filename: String): FeatureCollection =
        context.assets.open(filename).use {
            return FeatureCollection.fromJson(it.bufferedReader().use(BufferedReader::readText))  // Add 'return'
        }


    fun getFirstThreeTimelineItems(events: List<LocationData>): List<TimelineItem> {
        val preparedItems = prepareTimelineItems(events)

        val result = mutableListOf<TimelineItem>()
        var eventsAdded = 0

        for (item in preparedItems) {
            if (item is TimelineItem.EventItem) {
                result.add(item)
                eventsAdded++
                if (eventsAdded == 3) break
            } else if (item is TimelineItem.DateLabel && eventsAdded < 3) {
                if (result.isEmpty() || result.last() !is TimelineItem.DateLabel) {
                    result.add(item)
                }
            }
        }
        return result
    }


    fun buildAppAccessStatsFromGeoJson(context: Context): List<AppAccessStats> {
        val geoJsonString = context.assets.open(AppState.selectedGeoJsonFile).bufferedReader().use { it.readText() }
        val featureCollection = FeatureCollection.fromJson(geoJsonString)
        val features = featureCollection.features() ?: return emptyList()

        val accessStatsMap = features.groupBy { it.getStringProperty("appName") }
            .mapValues { entry ->
                val totalAccesses = entry.value.size
                val numberOfPOIs = calculatePOIs(entry.value)

                // Initialize stats object with all parameters except the privacy score
                val initialStats = AppAccessStats(
                    appName = entry.key,
                    totalAccesses = totalAccesses,
                    days = BaseActivity.AppSettings.daysFilter,
                    approximateAccesses = entry.value.count { it.getStringProperty("usageType") == "approximate" },
                    preciseAccesses = entry.value.count { it.getStringProperty("usageType") == "precise" },
                    foregroundAccesses = entry.value.count { it.getStringProperty("interactionType") == "foreground" },
                    backgroundAccesses = entry.value.count { it.getStringProperty("interactionType") == "background" },
                    subliminalAccesses = entry.value.count { it.getStringProperty("interactionType") == "subliminal" },
                    preciseForegroundAccesses = entry.value.count { it.getStringProperty("usageType") == "precise" && it.getStringProperty("interactionType") == "foreground" },
                    approximateForegroundAccesses = entry.value.count { it.getStringProperty("usageType") == "approximate" && it.getStringProperty("interactionType") == "foreground" },
                    preciseBackgroundAccesses = entry.value.count { it.getStringProperty("usageType") == "precise" && it.getStringProperty("interactionType") == "background" },
                    approximateBackgroundAccesses = entry.value.count { it.getStringProperty("usageType") == "approximate" && it.getStringProperty("interactionType") == "background" },
                    preciseSubliminalAccesses = entry.value.count { it.getStringProperty("usageType") == "precise" && it.getStringProperty("interactionType") == "subliminal" },
                    approximateSubliminalAccesses = entry.value.count { it.getStringProperty("usageType") == "approximate" && it.getStringProperty("interactionType") == "subliminal" },
                    numberOfPOIs = numberOfPOIs,
                    privacyScore = 0.0  // Placeholder, will be calculated next
                )

                // Calculate the privacy score and update the stats object
                val privacyScore = calculatePrivacyScore(initialStats)
                val updatedStats = initialStats.copy(privacyScore = privacyScore)

                // Log the final stats including the correctly calculated privacy score
                Log.d("AppStats", "App: ${updatedStats.appName}, Privacy Score: ${updatedStats.privacyScore}, Number of POIs: ${updatedStats.numberOfPOIs}")

                updatedStats
            }.values.toList()

        AppState.topAccessedAppsCache = accessStatsMap.sortedByDescending { it.totalAccesses }
        return AppState.topAccessedAppsCache!!
    }


    fun calculatePrivacyScore(stats: AppAccessStats): Double {


        // Base penalties for subliminal, background, and precise accesses per day
        val subliminalPenalty = minOf(35.0, (stats.subliminalAccesses.toDouble() / stats.days) * 10)
        val backgroundPenalty = minOf(35.0, (stats.backgroundAccesses.toDouble() / stats.days) * 3)
        val precisePenalty = minOf(15.0, (stats.preciseAccesses.toDouble() / stats.days) * 3)

        // Additional detailed frequencies
        val preciseBackgroundFreq = stats.preciseBackgroundAccesses.toDouble() / stats.days
        val preciseSubliminalFreq = stats.preciseSubliminalAccesses.toDouble() / stats.days
        val approximateSubliminalFreq = stats.approximateSubliminalAccesses.toDouble() / stats.days
        val freqPenalty = preciseBackgroundFreq + preciseSubliminalFreq + approximateSubliminalFreq

        // Incorporating POI impact
        val poiImpact = minOf(15.0, 5 * minOf(3, stats.numberOfPOIs).toDouble())

        // Calculating the final privacy score
        var score = 100.0
        score -= minOf(35.0, freqPenalty) // cap frequency impact at 35 points
        score -= subliminalPenalty
        score -= backgroundPenalty
        score -= precisePenalty
        score -= poiImpact

        // Ensure score is within 0-100
        return maxOf(0.0, score)
    }

    fun calculatePOIs(features: List<Feature>, threshold: Int = 100, radius: Double = 30.0): Int {
        // Convert features to points
        val points = features.mapNotNull { it.geometry() as? Point }
        val visited = BooleanArray(points.size)
        var poiCount = 0

        for (i in points.indices) {
            if (!visited[i]) {
                var count = 1
                for (j in points.indices) {
                    if (i != j && !visited[j] && points[i].distanceTo(points[j]) <= radius) {
                        count++
                        visited[j] = true
                    }
                }
                if (count >= threshold) {
                    poiCount++
                    visited[i] = true
                }
            }
        }
        return poiCount
    }

    fun Point.distanceTo(other: Point): Double {
        val earthRadius = 6371000 // m
        val dLat = Math.toRadians(other.latitude() - this.latitude())
        val dLon = Math.toRadians(other.longitude() - this.longitude())
        val lat1 = Math.toRadians(this.latitude())
        val lat2 = Math.toRadians(other.latitude())

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }






}


