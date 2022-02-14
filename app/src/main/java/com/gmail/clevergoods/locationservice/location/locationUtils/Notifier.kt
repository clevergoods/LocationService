package com.gmail.clevergoods.locationservice.location.locationUtils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gmail.clevergoods.MainActivity
import com.gmail.clevergoods.R
import com.gmail.clevergoods.WLog

class Notifier(private val ctx: Context) {

    private val LOG_TAG = "Notifier"
    companion object {
        val NOTIFY_ID = 1
    }

    fun sendNotification(title: String?, message: String?, intent: PendingIntent?,
                         isSound: Boolean, isAutoCancel: Boolean, NOTIFY_ID: Int) {
        val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var NOTIFICATION_CHANNEL_ID = "wl_channel_id_01"
        if (!isSound) {
            NOTIFICATION_CHANNEL_ID = "wl_channel_id_02"
        }
        val notificationBuilder = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "WL Server " +
                    "Notification", NotificationManager.IMPORTANCE_HIGH)
            if (!isSound) {
                notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "WL Server Notification",
                        NotificationManager.IMPORTANCE_LOW)
                notificationChannel.setSound(null, null)
            }
            notificationChannel.description = "WL channel"
            notificationChannel.enableLights(true)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationChannel.lightColor = Color.RED
            notificationManager.createNotificationChannel(notificationChannel)
        }
        notificationBuilder
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(isAutoCancel)
                .setOngoing(!isAutoCancel)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        if (intent != null) {
            WLog.d(LOG_TAG, "notificationBuilder.setContentIntent")
            notificationBuilder.setContentIntent(intent)
        }
        if (isSound) {
            WLog.d(LOG_TAG, "notificationBuilder.setSound")
            WLog.d(LOG_TAG, "notificationBuilder.setPriority(IMPORTANCE_HIGH)")
            notificationBuilder.priority = NotificationManagerCompat.IMPORTANCE_HIGH
            notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            WLog.d(LOG_TAG, "notificationBuilder.setPriority(IMPORTANCE_LOW)")
            notificationBuilder.priority = NotificationManagerCompat.IMPORTANCE_LOW
        }
        val notification = notificationBuilder.build()
        notificationManager.notify(NOTIFY_ID, notification)
    }

    fun getNotification(title: String?, message: String?, intent: PendingIntent?,
                        isSound: Boolean, isAutoCancel: Boolean): Notification {
        val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var NOTIFICATION_CHANNEL_ID = "wl_channel_id_01"
        if (!isSound) {
            NOTIFICATION_CHANNEL_ID = "wl_channel_id_02"
        }
        val notificationBuilder = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "WL Server " +
                    "Notification", NotificationManager.IMPORTANCE_HIGH)
            if (!isSound) {
                notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "WL Server Notification",
                        NotificationManager.IMPORTANCE_LOW)
                notificationChannel.setSound(null, null)
            }
            notificationChannel.description = "WL channel"
            notificationChannel.enableLights(true)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationChannel.lightColor = Color.RED
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val largeIcon = BitmapFactory.decodeResource(ctx.resources, R.drawable.ic_launcher)
        notificationBuilder
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(isAutoCancel)
                .setOngoing(!isAutoCancel)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        if (intent != null) {
            WLog.d(LOG_TAG, "notificationBuilder.setContentIntent")
            notificationBuilder.setContentIntent(intent)
        }
        if (isSound) {
            WLog.d(LOG_TAG, "notificationBuilder.setSound")
            WLog.d(LOG_TAG, "notificationBuilder.setPriority(IMPORTANCE_HIGH)")
            notificationBuilder.priority = NotificationManagerCompat.IMPORTANCE_HIGH
            notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            WLog.d(LOG_TAG, "notificationBuilder.setPriority(IMPORTANCE_LOW)")
            notificationBuilder.priority = NotificationManagerCompat.IMPORTANCE_LOW
        }
        return notificationBuilder.build()
    }

    fun notifyMessage(title: String?, message: String?, intent: PendingIntent?) {
        if (MainActivity.isWatchApp) {
            return
        }
        sendNotification(title, message, intent, true, true, NOTIFY_ID)
    }

    fun notifyTestMessage(title: String?, message: String?, intent: PendingIntent?) {
        val notification = getNotification(title, message, intent, false, true)
        sendNotification(title, message, intent, false, true, NOTIFY_ID)
    }

    fun notifyGeoMessage(title: String?, message: String?, intent: PendingIntent?) {
        if (MainActivity.isWatchApp) {
            return
        }
        sendNotification(title, message, intent, false, false, NOTIFY_ID + 1)
    }

    fun notifyWarningMessage(title: String?, message: String?, intent: PendingIntent?) {
        if (MainActivity.isWatchApp) {
            return
        }
        sendNotification(title, message, intent, false, true, NOTIFY_ID + 2)
    }

    fun notifySurveyMessage(title: String?, message: String?, intent: PendingIntent?) {
        if (MainActivity.isWatchApp) {
            return
        }
        sendNotification(title, message, intent, true, true, NOTIFY_ID + 3)
    }

    fun notifySerialMessage(title: String?, message: String?, intent: PendingIntent?) {
        if (MainActivity.isWatchApp) {
            return
        }
        sendNotification(title, message, intent, true, true, NOTIFY_ID + 4)
    }
}