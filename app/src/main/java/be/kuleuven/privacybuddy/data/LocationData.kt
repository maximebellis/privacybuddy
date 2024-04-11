package be.kuleuven.privacybuddy.data

import android.os.Parcel
import android.os.Parcelable
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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        Date(parcel.readLong()),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readString(),
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readValue(Double::class.java.classLoader) as? Double
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(timestamp)
        parcel.writeLong(date.time)
        parcel.writeString(appName)
        parcel.writeString(usageType)
        parcel.writeString(interactionType)
        parcel.writeValue(accuracy)
        parcel.writeValue(speed)
        parcel.writeValue(bearing)
        parcel.writeString(screenState)
        parcel.writeValue(latitude)
        parcel.writeValue(longitude)
        parcel.writeValue(altitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LocationData> {
        override fun createFromParcel(parcel: Parcel): LocationData {
            return LocationData(parcel)
        }

        override fun newArray(size: Int): Array<LocationData?> {
            return arrayOfNulls(size)
        }
    }
}