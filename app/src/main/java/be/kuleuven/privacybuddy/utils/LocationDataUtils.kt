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

        val accessStatsList = features.groupBy { it.getStringProperty("appName") }
            .map { (appName, featuresList) -> // Here we capture the grouped features list correctly
                val numberOfPOIs = calculatePOIs(featuresList) // Assuming calculatePOIs is correctly implemented

                AppAccessStats(
                    appName = appName,
                    totalAccesses = featuresList.size,
                    days = 21, // Example days filter
                    approximateAccesses = featuresList.count { it.getStringProperty("usageType") == "approximate" },
                    preciseAccesses = featuresList.count { it.getStringProperty("usageType") == "precise" },
                    foregroundAccesses = featuresList.count { it.getStringProperty("interactionType") == "foreground" },
                    backgroundAccesses = featuresList.count { it.getStringProperty("interactionType") == "background" },
                    subliminalAccesses = featuresList.count { it.getStringProperty("interactionType") == "subliminal" },
                    preciseForegroundAccesses = featuresList.count { it.usageAndInteraction("precise", "foreground") },
                    approximateForegroundAccesses = featuresList.count { it.usageAndInteraction("approximate", "foreground") },
                    preciseBackgroundAccesses = featuresList.count { it.usageAndInteraction("precise", "background") },
                    approximateBackgroundAccesses = featuresList.count { it.usageAndInteraction("approximate", "background") },
                    preciseSubliminalAccesses = featuresList.count { it.usageAndInteraction("precise", "subliminal") },
                    approximateSubliminalAccesses = featuresList.count { it.usageAndInteraction("approximate", "subliminal") },
                    numberOfPOIs = numberOfPOIs,
                    privacyScore = 0.0  // Placeholder, will be calculated next
                ).also {
                    it.privacyScore = calculatePrivacyScore(it)
                }
            }
        accessStatsList.forEach { app ->
            app.privacyScore = calculatePrivacyScore(app)
        }
        accessStatsList.forEach { app ->
            app.privacyScore = normalizeScore(app.privacyScore, -37.0, 0.0, 0.0, 100.0)
            Log.d("AppStats", app.toString())
        }

        AppState.topAccessedAppsCache = accessStatsList.sortedByDescending { it.totalAccesses }

        return AppState.topAccessedAppsCache!!
    }

    fun calculatePrivacyScore(stats: AppAccessStats): Double {
        // Calculate daily frequency of each access type
        val dailyPreciseForeground = stats.preciseForegroundAccesses.toDouble() / stats.days
        val dailyApproximateForeground = stats.approximateForegroundAccesses.toDouble() / stats.days
        val dailyPreciseBackground = stats.preciseBackgroundAccesses.toDouble() / stats.days
        val dailyApproximateBackground = stats.approximateBackgroundAccesses.toDouble() / stats.days
        val dailyPreciseSubliminal = stats.preciseSubliminalAccesses.toDouble() / stats.days
        val dailyApproximateSubliminal = stats.approximateSubliminalAccesses.toDouble() / stats.days

        // Assign penalties
        val penaltyPreciseForeground = dailyPreciseForeground * 0.1  // lower penalty for foreground
        val penaltyApproximateForeground = dailyApproximateForeground * 0.05
        val penaltyPreciseBackground = dailyPreciseBackground * 0.2  // higher penalty for background
        val penaltyApproximateBackground = dailyApproximateBackground * 0.1
        val penaltyPreciseSubliminal = dailyPreciseSubliminal * 0.4  // highest penalty for subliminal
        val penaltyApproximateSubliminal = dailyApproximateSubliminal * 0.2

        // Total penalty is the sum of all penalties
        val totalPenalty = penaltyPreciseForeground + penaltyApproximateForeground +
                penaltyPreciseBackground + penaltyApproximateBackground +
                penaltyPreciseSubliminal + penaltyApproximateSubliminal

        // Subtract the penalty and the impact of points of interest from the base score of 100
        return 0 - (totalPenalty + stats.numberOfPOIs * 5)
    }



    private fun normalizeScore(oldScore: Double, minOld: Double, maxOld: Double, minNew: Double, maxNew: Double): Double {
        return ((oldScore - minOld) / (maxOld - minOld)) * (maxNew - minNew) + minNew
    }

    private fun Feature.usageAndInteraction(usageType: String, interactionType: String): Boolean {
        return getStringProperty("usageType") == usageType && getStringProperty("interactionType") == interactionType
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


