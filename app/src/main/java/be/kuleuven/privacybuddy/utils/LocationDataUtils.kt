package be.kuleuven.privacybuddy.utils

import android.content.Context
import android.graphics.Color
import android.util.Log
import be.kuleuven.privacybuddy.AppState
import be.kuleuven.privacybuddy.AppState.globalData
import be.kuleuven.privacybuddy.AppState.selectedGeoJsonFile
import be.kuleuven.privacybuddy.AppState.selectedInteractionTypes
import be.kuleuven.privacybuddy.AppState.selectedUsageTypes
import be.kuleuven.privacybuddy.BaseActivity
import be.kuleuven.privacybuddy.adapter.TimelineItem
import be.kuleuven.privacybuddy.data.AppAccessStats
import be.kuleuven.privacybuddy.data.LocationData
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    fun cacheAllLocationData(context: Context, filename: String = selectedGeoJsonFile, days: Int = BaseActivity.AppSettings.daysFilter): List<LocationData> {
        return try {
            val featureCollection = parseGeoJsonFromAssets(context, filename)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            val cutoffDate = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000

            globalData = featureCollection.features()?.mapNotNull { feature ->
                val timestamp = feature.getStringProperty("timestamp")
                val date = dateFormat.parse(timestamp)

                val point = feature.geometry() as? Point
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
            }?.filter { data ->
                (selectedUsageTypes.isEmpty() || selectedUsageTypes.contains(data.usageType)) &&
                        (selectedInteractionTypes.isEmpty() || selectedInteractionTypes.contains(data.interactionType)) &&
                        (data.date.time >= cutoffDate)
            } ?: emptyList()
            Log.d("LocationDataUtils", "Location data cached: ${globalData.toString()}")
            globalData
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
        Log.d("LocationDataUtils", "GeoJSON data loaded")
        val featureCollection = FeatureCollection.fromJson(geoJsonString)
        val features = featureCollection.features() ?: return emptyList()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        val cutoffDate = LocalDateTime.now(ZoneOffset.UTC).minusDays(BaseActivity.AppSettings.daysFilter.toLong())

        val accessStatsList = features.filter {
            LocalDateTime.parse(it.getStringProperty("timestamp"), formatter) >= cutoffDate
        }.groupBy { it.getStringProperty("appName") }
            .map { (appName, filteredFeatures) ->
                val numberOfPOIs = calculatePOIs(filteredFeatures)

                AppAccessStats(
                    appName = appName,
                    totalAccesses = filteredFeatures.size,
                    days = BaseActivity.AppSettings.daysFilter,
                    approximateAccesses = filteredFeatures.count { it.getStringProperty("usageType") == "approximate" },
                    preciseAccesses = filteredFeatures.count { it.getStringProperty("usageType") == "precise" },
                    foregroundAccesses = filteredFeatures.count { it.getStringProperty("interactionType") == "foreground" },
                    backgroundAccesses = filteredFeatures.count { it.getStringProperty("interactionType") == "background" },
                    subliminalAccesses = filteredFeatures.count { it.getStringProperty("interactionType") == "subliminal" },
                    preciseForegroundAccesses = filteredFeatures.count { it.usageAndInteraction("precise", "foreground") },
                    approximateForegroundAccesses = filteredFeatures.count { it.usageAndInteraction("approximate", "foreground") },
                    preciseBackgroundAccesses = filteredFeatures.count { it.usageAndInteraction("precise", "background") },
                    approximateBackgroundAccesses = filteredFeatures.count { it.usageAndInteraction("approximate", "background") },
                    preciseSubliminalAccesses = filteredFeatures.count { it.usageAndInteraction("precise", "subliminal") },
                    approximateSubliminalAccesses = filteredFeatures.count { it.usageAndInteraction("approximate", "subliminal") },
                    numberOfPOIs = numberOfPOIs,
                    privacyScore = 0.0  // Placeholder, will be calculated next
                ).also {
                    it.privacyScore = calculatePrivacyScore(it)
                }
            }
        accessStatsList.forEach { app ->
            app.privacyScore = normalizeScore(app.privacyScore, 82.0, 100.0, 0.0, 100.0)
            Log.d("AppStats", app.toString())
        }

        AppState.topAccessedAppsCache = accessStatsList.sortedByDescending { it.totalAccesses }

        return AppState.topAccessedAppsCache!!
    }

    fun calculatePrivacyScore(stats: AppAccessStats): Double {
        val dailyPreciseForeground = stats.preciseForegroundAccesses.toDouble() / stats.days
        val dailyApproximateForeground = stats.approximateForegroundAccesses.toDouble() / stats.days
        val dailyPreciseBackground = stats.preciseBackgroundAccesses.toDouble() / stats.days
        val dailyApproximateBackground = stats.approximateBackgroundAccesses.toDouble() / stats.days
        val dailyPreciseSubliminal = stats.preciseSubliminalAccesses.toDouble() / stats.days
        val dailyApproximateSubliminal = stats.approximateSubliminalAccesses.toDouble() / stats.days

        val peopleAsked = 2
        val weightPreciseForeground = 0.1
        val weightPreciseBackground = weightPreciseForeground * (3+3)/peopleAsked
        val weightPreciseSubliminal = weightPreciseForeground * (10+8)/peopleAsked
        val relativeWeightApproxToPrecise = (0.2+0.5)/peopleAsked

        val penaltyPreciseForeground = dailyPreciseForeground * weightPreciseForeground
        val penaltyApproximateForeground = dailyApproximateForeground * relativeWeightApproxToPrecise
        val penaltyPreciseBackground = dailyPreciseBackground * weightPreciseBackground
        val penaltyApproximateBackground = dailyApproximateBackground * relativeWeightApproxToPrecise
        val penaltyPreciseSubliminal = dailyPreciseSubliminal * weightPreciseSubliminal
        val penaltyApproximateSubliminal = dailyApproximateSubliminal * relativeWeightApproxToPrecise

        val totalPenalty = penaltyPreciseForeground + penaltyApproximateForeground +
                penaltyPreciseBackground + penaltyApproximateBackground +
                penaltyPreciseSubliminal + penaltyApproximateSubliminal

        return 100 - (totalPenalty + stats.numberOfPOIs * 7)
    }

    fun getPrivacyScoreColor(score: Double): Int {
        val colorStops = arrayOf(
            Pair(100.0, Color.parseColor("#4CAF50")), // Vibrant Green
            Pair(80.0, Color.parseColor("#8BC34A")), // Light Green
            Pair(60.0, Color.parseColor("#CDDC39")), // Lime
            Pair(40.0, Color.parseColor("#FFEB3B")), // Bright Yellow
            Pair(20.0, Color.parseColor("#FFC107")), // Amber
            Pair(0.0, Color.parseColor("#F44336"))   // Bright Red
        )

        val lowerStop = colorStops.last { it.first <= score }
        val upperStop = colorStops.first { it.first >= score }

        val ratio = (score - lowerStop.first) / (upperStop.first - lowerStop.first)

        return interpolateColor(lowerStop.second, upperStop.second, ratio.toFloat())
    }

    private fun interpolateColor(colorStart: Int, colorEnd: Int, fraction: Float): Int {
        val startA = Color.alpha(colorStart)
        val startR = Color.red(colorStart)
        val startG = Color.green(colorStart)
        val startB = Color.blue(colorStart)

        val endA = Color.alpha(colorEnd)
        val endR = Color.red(colorEnd)
        val endG = Color.green(colorEnd)
        val endB = Color.blue(colorEnd)

        val newA = interpolate(startA, endA, fraction)
        val newR = interpolate(startR, endR, fraction)
        val newG = interpolate(startG, endG, fraction)
        val newB = interpolate(startB, endB, fraction)

        return Color.argb(newA, newR, newG, newB)
    }

    private fun interpolate(start: Int, end: Int, fraction: Float): Int {
        return (start + (end - start) * fraction).toInt()
    }



    private fun normalizeScore(oldScore: Double, minOld: Double, maxOld: Double, minNew: Double, maxNew: Double): Double {
        val score =  ((oldScore - minOld) / (maxOld - minOld)) * (maxNew - minNew) + minNew
        return Math.max(0.0, Math.min(100.0, score))
    }

    private fun Feature.usageAndInteraction(usageType: String, interactionType: String): Boolean {
        return getStringProperty("usageType") == usageType && getStringProperty("interactionType") == interactionType
    }



    fun calculatePOIs(features: List<Feature>, threshold: Int = 100, radius: Double = 30.0): Int {
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


