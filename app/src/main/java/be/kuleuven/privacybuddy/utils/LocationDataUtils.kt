package be.kuleuven.privacybuddy.utils

import android.content.Context
import android.util.Log
import be.kuleuven.privacybuddy.AppState
import be.kuleuven.privacybuddy.adapter.TimelineItem
import be.kuleuven.privacybuddy.data.AppAccessStats
import be.kuleuven.privacybuddy.data.LocationData
import com.mapbox.geojson.FeatureCollection
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Calendar
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


    fun buildAppAccessStatsFromGeoJson(context: Context, days: Int = 21): List<AppAccessStats> {
        val geoJsonString = context.assets.open(AppState.selectedGeoJsonFile).bufferedReader().use { it.readText() }
        val featureCollection = FeatureCollection.fromJson(geoJsonString)
        val features = featureCollection.features() ?: return emptyList()

        val accessStatsMap = features.groupBy { it.getStringProperty("appName") }
            .mapValues { entry ->
                val totalAccesses = entry.value.size
                val frequencyPerDay = totalAccesses.toFloat() / days
                val approximateAccesses = entry.value.count { it.getStringProperty("usageType") == "approximate" }
                val preciseAccesses = entry.value.count { it.getStringProperty("usageType") == "precise" }

                val foregroundAccesses = entry.value.count { it.getStringProperty("interactionType") == "foreground" }
                val backgroundAccesses = entry.value.count { it.getStringProperty("interactionType") == "background" }
                val subliminalAccesses = entry.value.count { it.getStringProperty("interactionType") == "subliminal" }

                val preciseForegroundAccesses = entry.value.count {
                    it.getStringProperty("usageType") == "precise" && it.getStringProperty("interactionType") == "foreground"
                }
                val approximateForegroundAccesses = entry.value.count {
                    it.getStringProperty("usageType") == "approximate" && it.getStringProperty("interactionType") == "foreground"
                }

                val preciseBackgroundAccesses = entry.value.count {
                    it.getStringProperty("usageType") == "precise" && it.getStringProperty("interactionType") == "background"
                }
                val approximateBackgroundAccesses = entry.value.count {
                    it.getStringProperty("usageType") == "approximate" && it.getStringProperty("interactionType") == "background"
                }

                val preciseSubliminalAccesses = entry.value.count {
                    it.getStringProperty("usageType") == "precise" && it.getStringProperty("interactionType") == "subliminal"
                }
                val approximateSubliminalAccesses = entry.value.count {
                    it.getStringProperty("usageType") == "approximate" && it.getStringProperty("interactionType") == "subliminal"
                }

                AppAccessStats(
                    appName = entry.key,
                    totalAccesses = totalAccesses,
                    frequencyPerDay = frequencyPerDay,
                    approximateAccesses = approximateAccesses,
                    preciseAccesses = preciseAccesses,
                    foregroundAccesses = foregroundAccesses,
                    backgroundAccesses = backgroundAccesses,
                    subliminalAccesses = subliminalAccesses,
                    preciseForegroundAccesses = preciseForegroundAccesses,
                    approximateForegroundAccesses = approximateForegroundAccesses,
                    preciseBackgroundAccesses = preciseBackgroundAccesses,
                    approximateBackgroundAccesses = approximateBackgroundAccesses,
                    preciseSubliminalAccesses = preciseSubliminalAccesses,
                    approximateSubliminalAccesses = approximateSubliminalAccesses
                )
            }.values.toList()


        // Sort the list by totalAccesses in descending order before caching and returning
        AppState.topAccessedAppsCache = accessStatsMap.sortedByDescending { it.totalAccesses }
        return AppState.topAccessedAppsCache!!
    }





}


