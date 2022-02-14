package com.gmail.clevergoods.locationservice.location;

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
import com.gmail.clevergoods.Utils
import com.gmail.clevergoods.Utils.Companion.isServiceRunning
import com.gmail.clevergoods.WLog
import com.gmail.clevergoods.location.LocationUtils.Companion.getInstance
import com.gmail.clevergoods.locationservice.location.service.LocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LocationWorkManager(private val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val interval =  inputData.getLong("interval", 60)
        val period =  inputData.getLong("period", 15*60)
        var result = Result.success()
        try {
            if (!isServiceRunning(LocationService::class.java, context) && Utils.isLocationServiceAllowed(context)) {
                WLog.d(LOG_TAG, "requestShortLocationUpdates with interval=$interval sec")
                GlobalScope.launch(Dispatchers.Main) {
                    getInstance(context).requestShortLocationUpdates(interval * 1000L, period * 1000L, PRIORITY_BALANCED_POWER_ACCURACY)
                }
            }
        } catch (e: Exception) {
            WLog.e(LOG_TAG + ".doWork", e)
            result = Result.failure()
        }
        return result
    }

    companion object {
        private const val LOG_TAG = "LocationWorkManager"
    }

}