package com.example.scoutconnections.notifications

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build

class NotificacionOreoUpper(base: Context?) : ContextWrapper(base) {

    val ID = "an_id"
    val NAME = "ScoutsConnections"
    var notificationManager: NotificationManager? = null

    init {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            createChannel()
        }
    }
    @TargetApi (Build.VERSION_CODES.O)
    private fun createChannel() {
        val notificationChannel = NotificationChannel(ID,NAME, NotificationManager.IMPORTANCE_DEFAULT)
        notificationChannel.enableLights(true)
        notificationChannel.enableVibration(true)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        getManager().createNotificationChannel(notificationChannel)
    }

    private fun getManager(): NotificationManager {
        if(notificationManager == null){
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return notificationManager as NotificationManager
    }

    @TargetApi(Build.VERSION_CODES.O)
    public fun getNotifications(title: String, body: String, pIntent: PendingIntent, soundUri: Uri, icon: String): Notification.Builder{
        return Notification.Builder(applicationContext, ID).setContentIntent(pIntent).setContentTitle(title).setContentText(body).setSound(soundUri).setAutoCancel(true).setSmallIcon(icon.toInt())
    }
}