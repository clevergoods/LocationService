package com.gmail.clevergoods.locationservice.location.data

import android.location.Location
import com.google.android.gms.location.LocationRequest
import com.msk.socmoncore.utils.IndoorDevice
import com.gmail.clevergoods.locationservice.location.service.LocationService
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class WLLocation {
    constructor() {}

    fun setIndoorDevices(indoorDevices: Set<IndoorDevice>?) {
        this.indoorDevices = indoorDevices
    }

    fun setBatteryLevel(batteryLevel: Byte) {
        this.batteryLevel = if (batteryLevel > 100) 100 else batteryLevel
    }

    fun setInvis(invis: Boolean) {
        this.invis = invis
    }

    fun setMovingState(){
        val trackInterval = PrefHelper.getTrackMode()
        movingState = ((LocationService.movingState.get() + 1) * 100 / (trackInterval * 1000 / LocationService.movingDelay)).toInt()
        LocationService.movingState.set(-1)
    }

    internal constructor(loc: Location) {
        time = loc.time / 1000
        lat = loc.latitude
        lon = loc.longitude
        speed = loc.speed
        alt = loc.altitude.toFloat()
        accuracy = loc.accuracy
        source = getSource(loc.provider)
        satCount = -1
        if (loc.extras != null) {
            if(loc.extras.containsKey("satellites")) satCount = loc.extras["satellites"] as Byte
            if(loc.extras.containsKey("priority")) priority = loc.extras["priority"] as Int
        }
        setMovingState()
    }

    var id = 0
    var time = 0L
    var lat = 0.0
    var lon = 0.0
    private var speed = 0f
    private var alt = 0f
    private var accuracy = 0f
    private var satCount: Byte = 0
    private var batteryLevel: Byte = 0
    var movingState = -1
    var priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    var source: Byte = 0
    private var invis = false
    private var indoorDevices: Set<IndoorDevice>? = null
    var sendImmediately = false

    fun serialize(): JSONObject? {//save to locations3 table
        return try {
            val obj = JSONObject()
            obj.put("time", time)
            obj.put("lat", lat)
            obj.put("lon", lon)
            obj.put("speed", speed.toDouble())
            obj.put("alt", alt.toDouble())
            obj.put("acc", accuracy.toDouble())
            obj.put("sat", satCount.toInt())
            obj.put("bat", batteryLevel.toInt())
            obj.put("src", source.toInt())
            obj.put("movingState", movingState)
            obj.put("priority", priority)
            obj.put("inv", if (invis) 1 else 0)

            if (indoorDevices != null && !indoorDevices!!.isEmpty()) {
                val array = JSONArray()
                for (item in indoorDevices!!) {
                    array.put(item.serialize())
                }
                obj.put("indoorDevices", array)
            }
            obj
        } catch (e: Exception) {
            WLog.e(LOG_TAG, e)
            null
        }
    }

    companion object {
        const val LOG_TAG = "WLTracker"
        private fun getSource(provider: String): Byte {
            return if (provider == "gps") 0 else if (provider == "network") 1 else 2
        }

        fun getLocationToSend(dataJson: JSONObject): JSONObject? {
            val jLocation = JSONObject()
            try {
                val keyList = ArrayList(Arrays.asList("time", "lat", "lon", "acc", "alt", "bat", "src", "sat", "inv"))
                for (key in keyList) {
                    if (dataJson.has(key) && dataJson[key].toString().toDouble() > 0) jLocation.put(key, dataJson[key])
                }
                if (dataJson.has("indoorDevices")) {
                    jLocation.put("indoorDevices", dataJson["indoorDevices"])
                }
                if (dataJson.has("priority")) {
                    jLocation.put("priority", dataJson["priority"])
                }
                if (dataJson.has("movingState")) {
                    jLocation.put("movingState", dataJson["movingState"])
                }
                return jLocation
            } catch (ex: JSONException) {
                WLog.e(LOG_TAG, ex)
            }
            return null
        }
    }
}