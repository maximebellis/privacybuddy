package be.kuleuven.privacybuddy.utils

import android.content.Context
import android.util.Log
import be.kuleuven.privacybuddy.AppState
import be.kuleuven.privacybuddy.adapter.TimelineItem
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
                dateFormat.parse(timestamp)?.takeIf {
                    it.after(cutoffDate) && (selectedAppName == null || feature.getStringProperty("appName") == selectedAppName)
                }?.let { date ->
                    LocationData(
                        timestamp,
                        date,
                        feature.getStringProperty("appName"),
                        feature.getStringProperty("usageType"),
                        feature.getStringProperty("interactionType")
                    )
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("LocationDataUtils", "Error loading or parsing data", e)
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

}


