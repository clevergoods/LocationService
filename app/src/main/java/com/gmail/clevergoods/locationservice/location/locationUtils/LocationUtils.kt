package com.gmail.clevergoods.locationservice.location.locationUtils;

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Looper
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
import com.google.android.gms.tasks.OnSuccessListener
import com.gmail.clevergoods.LocationUpdatesBroadcastReceiver
import com.gmail.clevergoods.PrefHelper
import com.gmail.clevergoods.WLog
import com.gmail.clevergoods.locationservice.location.LocationWorkManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.TimeUnit


class LocationUtils {
    companion object {
        @Volatile
        private var INSTANCE: LocationUtils? = null
        private const val priority = PRIORITY_BALANCED_POWER_ACCURACY//PRIORITY_HIGH_ACCURACY //PRIORITY_BALANCED_POWER_ACCURACY
        private var fusedLocationProviderClient: FusedLocationProviderClient? = null
        private val LOG_TAG = "LocationUtils"
        
        fun getInstance(context: Context): LocationUtils =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildLocationUtils(context).also { INSTANCE = it }
                }

        private fun buildLocationUtils(ctx: Context): LocationUtils {
            context = ctx
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(ctx)
            return LocationUtils(ctx)
        }

        fun stopLocationWorker(id: UUID?, ctx: Context) {
            id?.let{
                WorkManager.getInstance(ctx).cancelWorkById(id)
            }
        }

        fun startLocationWorker(ctx: Context, interval: Long): UUID? {
            var id: UUID? = null
            try {
                val data = Data.Builder()
                var period = 15*60L
                data.putLong("interval", interval)
                if(interval >= period)period=interval*2L
                data.putLong("period", period)
                val locationServiceRequest = PeriodicWorkRequest.Builder(LocationWorkManager::class.java, period, TimeUnit.SECONDS)
                        .setInputData(data.build())
                        .build()
                WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("LocationWorker", ExistingPeriodicWorkPolicy.REPLACE, locationServiceRequest)
                id = locationServiceRequest.id
            } catch (e: java.lang.Exception) {
                WLog.e(LOG_TAG, e)
            }
            return id
        }
    }

    private var locationRequest: LocationRequest? = null

    private fun getPendingIntent(): PendingIntent? {
        val intent = Intent(context, LocationUpdatesBroadcastReceiver::class.java)
        intent.action = LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createLocationRequest(interval: Long) {
        locationRequest = LocationRequest()
        WLog.i(LOG_TAG, "createLocationRequest trackInterval=$interval")
        val fastInterval = interval/4
        locationRequest!!.setInterval(interval)
                .setFastestInterval(fastInterval)
                .setPriority(priority)
    }

    fun requestLocationUpdates() {
        val trackingInterval = PrefHelper.getTrackMode()*1000L
        WLog.i(LOG_TAG, "requestLocationUpdates with interval=$trackingInterval")
        createLocationRequest(trackingInterval)
        fusedLocationProviderClient!!.requestLocationUpdates(locationRequest, getPendingIntent())
    }

    suspend fun requestShortLocationUpdates(interval:Long, period:Long, priority: Int):Boolean {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!.applicationContext)
        val locationRequest = LocationRequest()
        locationRequest.interval = interval*1000
        locationRequest.fastestInterval = interval/4
        locationRequest.priority = priority //according to your app
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                if (locationResult.locations.isNotEmpty()) {
                    //val location = locationResult.lastLocation
                    val intent = Intent(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES)
                    val locResult = LocationResult.create(locationResult.locations)
                    intent.putExtra("com.google.android.gms.location.EXTRA_LOCATION_RESULT", locResult)
                    intent.putExtra("com.gmail.clevergoods.SEND_IMMEDIATELY", true)
                    intent.putExtra("priority", priority)
                    try {
                        getPendingIntent()!!.send(context, 0, intent)
                    } catch (e: Exception) {
                        WLog.e(LOG_TAG, e.toString())
                    }
                }
            }
        }

        val value = GlobalScope.async {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper() /* Looper */
            )
            delay(period*1000)
            fusedLocationClient.removeLocationUpdates(locationCallback)
            WLog.i(LOG_TAG, "RequestShortLocationUpdates thread running on [${Thread.currentThread().name}]")
            true
        }
        return value.await()
    }

    fun requestLastLocation() {
        fusedLocationProviderClient!!.getLastLocation().addOnSuccessListener(OnSuccessListener<Location?> { location ->
            WLog.i(LOG_TAG, "requestLastLocation")

            if (location != null) {
                WLog.d(LOG_TAG, "requestLastLocation location= " + location.toString())

                val intent = Intent(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES)
                val locList: MutableList<Location> = ArrayList()
                locList.add(location)
                val locResult = LocationResult.create(locList)
                intent.putExtra("com.google.android.gms.location.EXTRA_LOCATION_RESULT", locResult)
                intent.putExtra("com.gmail.clevergoods.SEND_IMMEDIATELY", true)
                try {
                    getPendingIntent()!!.send(context, 0, intent)
                } catch (e: Exception) {
                    WLog.d(LOG_TAG, e.toString())
                }
            }
        })
    }

    fun removeLocationUpdates() {
        fusedLocationProviderClient!!.removeLocationUpdates(getPendingIntent())
        WLog.d(LOG_TAG, "RemoveLocationUpdates")
    }

    fun singleLocationRequest() {
        fusedLocationProviderClient!!.getLastLocation().addOnSuccessListener({ location ->
            locationRequest = LocationRequest()
            val trackingInterval: Long
            if (location != null) {
                WLog.d(LOG_TAG, "singleLocationRequest   getLastLocation time=${location.time}")
                trackingInterval = PrefHelper.getTrackMode() * 1000L
                removeLocationUpdates()
            } else {
                trackingInterval = 300 * 1000
            }

            locationRequest!!.setInterval(trackingInterval)
                    .setFastestInterval(1000)
                    .setNumUpdates(1)
                    .setPriority(priority)

            fusedLocationProviderClient!!.requestLocationUpdates(locationRequest, getPendingIntent())
        })
    }


    fun requestLocationUpdatesWithoutSending() {
        WLog.d(LOG_TAG, "requestLocationUpdatesWithoutSending")
        val trackingInterval = PrefHelper.getTrackMode()*1000L
        createLocationRequest(trackingInterval)

        val intent = Intent(context, LocationUpdatesBroadcastReceiver::class.java)
        intent.action = LocationUpdatesBroadcastReceiver.ACTION_JUST_UPDATE

        fusedLocationProviderClient!!.requestLocationUpdates(locationRequest, PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
    }
}