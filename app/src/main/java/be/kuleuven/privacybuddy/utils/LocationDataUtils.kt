package be.kuleuven.privacybuddy.utils

import android.content.Context
import android.util.Log
import be.kuleuven.privacybuddy.adapter.TimelineItem
import be.kuleuven.privacybuddy.data.LocationData
import com.mapbox.geojson.FeatureCollection
import java.io.BufferedReader
import java.text.SimpleDateFormat
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

    fun loadGeoJsonFromAssets(selectedAppName: String?, context: Context, filename:String = "dummy_location_data.geojson"): List<LocationData> {
        return try {
            val featureCollection = parseGeoJsonFromAssets(context, filename)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            featureCollection.features()?.mapNotNull { feature ->
                val timestamp = feature.getStringProperty("timestamp")
                val date = dateFormat.parse(timestamp)

                date?.let {
                    LocationData(
                        timestamp,
                        it,
                        feature.getStringProperty("appName"),
                        feature.getStringProperty("usageType"),
                        feature.getStringProperty("interactionType")
                    )
                }
            }?.filter { selectedAppName == null || it.appName == selectedAppName } ?: emptyList()
        } catch (e: Exception) {
            Log.e("LocationDataUtils", "Error loading or parsing data", e)
            emptyList()
        }
    }

    private fun parseGeoJsonFromAssets(context: Context, filename: String): FeatureCollection =
        context.assets.open(filename).use {
            FeatureCollection.fromJson(it.bufferedReader().use(BufferedReader::readText))
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


