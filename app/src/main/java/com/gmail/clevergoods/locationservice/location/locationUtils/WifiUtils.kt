package com.msk.socmoncore.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.gmail.clevergoods.Utils
import com.gmail.clevergoods.WLLocation
import com.gmail.clevergoods.WLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

class WifiUtils(
        val context: Context
) {
    val attemptCount = 2
    var count = AtomicInteger(0)
    val currentDate = System.currentTimeMillis()
    @Volatile
    var toSend: Boolean = false
    @Volatile
    lateinit var locationResult: WLLocation

    companion object {
        const val LOG_TAG = "app_wifi"
    }

    private val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            scanEnd()
            val wifi = wifiManager.scanResults
            val success = !wifi.isEmpty()
            if(success) {
                scanSuccess(wifi)
            } else {
                scanFailure()
            }
            WLog.d(LOG_TAG, "wifi scan result: ${success} for ${currentDate} - attempt ${count}")
        }
    }

    private val intentFilter: IntentFilter by lazy {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        intentFilter
    }

    fun wifiScan() {
        wifiScan(null, true)
    }

    fun wifiScan(locResult: WLLocation?, isSending: Boolean) {
        toSend = isSending
        if (locResult != null) {
            locationResult = locResult
        }
        context.applicationContext.registerReceiver(wifiScanReceiver, intentFilter)
        wifiManager.startScan()
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private fun scanEnd() {
        context.applicationContext.unregisterReceiver(wifiScanReceiver)
    }

    private fun scanSuccess(wifi: List<ScanResult>) {
        val result = wifi.asSequence()
                .map {
                    IndoorDevice(it.BSSID, it.level)
                }.toSet()
        coroutineScope.launch(Dispatchers.IO) {
            processWifi(result)
        }
    }

    private suspend fun processWifi(wifi: Set<IndoorDevice>) {
        if (wifi.isEmpty() && count.get() < attemptCount) {
            repeatScan()
        } else {
            sendLocation(wifi)
        }
    }

    private fun sendLocation(wifi: Set<IndoorDevice>) {
        locationResult.setIndoorDevices(wifi)
        Utils.getInstance(context).processLocation(context, locationResult)
    }

    private fun scanFailure() {
        coroutineScope.launch(Dispatchers.IO) {
            processWifi(setOf())
        }
    }

    suspend fun repeatScan() {
        count.incrementAndGet()
        delay(15000)
        if (::locationResult.isInitialized) {
            wifiScan(locationResult, toSend)
        } else {
            wifiScan()
        }
    }
}

data class IndoorDevice(
        val mac: String,
        val rssi: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IndoorDevice) return false

        if (mac != other.mac) return false

        return true
    }

    override fun hashCode(): Int {
        return mac.hashCode()
    }

    fun serialize(): JSONObject? {
        return try {
            val obj = JSONObject()
            obj.put("name", mac)
            obj.put("rssi", rssi)
        } catch (e: Exception) {
            null
        }
    }
}
