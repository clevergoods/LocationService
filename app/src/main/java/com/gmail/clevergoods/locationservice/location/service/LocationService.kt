package com.gmail.clevergoods.locationservice.location.service;

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.*
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.gmail.clevergoods.*
import com.gmail.clevergoods.LocationUpdatesBroadcastReceiver.LOG_LOCATION_SERVICE
import com.gmail.clevergoods.Utils.Companion.wrap
import com.gmail.clevergoods.locationservice.location.utils.LocationUtils
import com.gmail.clevergoods.locationservice.location.utils.Notifier
import org.qtproject.qt5.android.bindings.QtService
import java.util.concurrent.atomic.AtomicInteger

class LocationService : QtService(), SensorEventListener {
    companion object {
        private val LOG_TAG = "LocationService"
        var notification:Notification? = null
        val movingState = AtomicInteger(-1)
        val movingDelay = 5000L
        val gravityDelay = 500L
        val gravityCount = AtomicInteger(-1)
        val gravity = doubleArrayOf(0.0, 0.0, 0.0)
        var sensorManager:SensorManager? = null
    }
    var smSensor: Sensor? = null
    var amSensor: Sensor? = null
    var smTriggerEventListener: TriggerEventListener? = null
    var amTriggerEventListener: TriggerEventListener? = null
    private var outMessenger: Messenger? = null

    inner class LocationBinder : Binder() {
        fun getService(): LocationService? {
            return this@LocationService
        }
    }

    private val mBinder: IBinder = LocationBinder()
    override fun onBind(intent: Intent?): IBinder? {
        val extras: Bundle? = intent?.getExtras()
        extras?.let { outMessenger = it["MESSENGER"] as Messenger? }
        return mBinder
    }

    override fun onCreate() {
        WLog.d(LOG_TAG, "LOCATION SERVICE START")
        sendMessage("$LOG_TAG start")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = getNotification()
            startForeground(Notifier.NOTIFY_ID + 1, notification)
        }


        if(sensorManager == null) sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.let { smn->

            smSensor = smn.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)
            smSensor?.let {sns ->
                smTriggerEventListener = object : TriggerEventListener() {
                    override fun onTrigger(event: TriggerEvent?) {
                        if (event != null && event.values[0] > 0) {
                            movingState.getAndIncrement()
                            sendMessage("$LOG_TAG SIGNIFICANT_MOTION Sensor ${event.sensor.name} value = ${event.values[0]}, time = ${event.timestamp}")
                            Handler(Looper.getMainLooper()).postDelayed({
                                Thread(
                                        Runnable {
                                            sendMessage("$LOG_TAG SIGNIFICANT_MOTION = false")
                                            sns.also { sensor ->
                                                smn.requestTriggerSensor(smTriggerEventListener, sensor)
                                            }
                                        }
                                ).start()
                            }, movingDelay)
                        }
                    }
                }
                sns.also { sensor ->
                    smn.requestTriggerSensor(smTriggerEventListener, sensor)
                }
            }?: run {
                amSensor = smn.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                amSensor?.let {ans->
                    smn.registerListener(this, ans, SensorManager.SENSOR_DELAY_NORMAL);
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val ctx: Context = wrap(newBase)
        super.attachBaseContext(ctx)
    }

    private fun getNotification():Notification {
        val channelId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel("wl_channel_id_01", "Location Background Service")
                } else {
                    ""
                }

        // Настраиваем действие при нажатии - показывать наше приложение
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        notificationIntent.putExtra("qmlForm", "MapForm")
        val intent = PendingIntent.getActivity(this, System.currentTimeMillis().toInt(), notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification =
                notificationBuilder.setOngoing(true)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle(this.getString(R.string.notification_title))
                        .setContentText(this.getString(R.string.notification_text))
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .setContentIntent(intent)
                        .build()
        return notification
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
                channelId,
                channelName, NotificationManager.IMPORTANCE_LOW
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(Notifier.NOTIFY_ID + 1, notification)
        }
        WLog.i(LOG_TAG, "onStartCommand")
        sendMessage("$LOG_TAG onStartCommand")
        if (intent == null) {
            stopSelf()
        } else {
            val defInterval = if(BuildConfig.IS_WATCH_APP) 300 else 60
            PrefHelper.setTrackMode(intent.getIntExtra("trackInterval", defInterval))
            LocationUtils.getInstance(this).removeLocationUpdates()
            LocationUtils.getInstance(this).requestLocationUpdates()
            WLog.d(LOG_TAG, "LOCATION SERVICE RESTARTED")
            sendMessage("$LOG_TAG RESTARTED")
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        //смахнули приложение
        WLog.d(LOG_TAG, "TASK REMOVED")
        sendMessage("$LOG_TAG TASK REMOVED")
    }

    override fun onDestroy() {
        stopForeground(true)
        LocationUtils.getInstance(this).removeLocationUpdates()

        WLog.d(LOG_TAG, "LOCATION SERVICE STOP")
        sendMessage("$LOG_TAG STOP")
    }

    fun sendMessage(message: String) {
        val intent = Intent()
        intent.action = LOG_LOCATION_SERVICE
        intent.putExtra("message", message)
        applicationContext.sendBroadcast(intent)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val alpha = 0.8;

            val gravityX = alpha * gravity[0] + (1 - alpha) * event.values[0];
            val gravityY = alpha * gravity[1] + (1 - alpha) * event.values[1];
            val gravityZ = alpha * gravity[2] + (1 - alpha) * event.values[2];

            gravity[0] = gravityX
            gravity[1] = gravityY
            gravity[2] = gravityZ

            gravityCount.getAndIncrement()

            if (gravityCount.get() > 500) {
                gravityCount.getAndSet(-1)
                gravity[0] = 0.0
                gravity[1] = 0.0
                gravity[2] = 0.0
                val linearAccelerationX = event.values[0] - gravityX
                val linearAccelerationY = event.values[1] - gravityY
                val linearAccelerationZ = event.values[2] - gravityZ
                val la = (linearAccelerationX * linearAccelerationX + linearAccelerationY * linearAccelerationY + linearAccelerationZ * linearAccelerationZ).toInt()

                if (la > 0){
                    movingState.getAndIncrement()
                    sendMessage("$LOG_TAG onSensorChanged MOVING linearAcceleration = $la")
                }else{
                    sendMessage("$LOG_TAG onSensorChanged REST linearAcceleration = $la")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        WLog.d(LOG_TAG, "sensor type = ${sensor?.type} accuracy = $accuracy")
    }
}