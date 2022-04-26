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

class NotificacionOreoEncima(base: Context?) : ContextWrapper(base) {

    val ID = "un_id"
    val NOMBRE = "ScoutsConnections"
    var notificacionManager: NotificationManager? = null

    init {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            crearCanal()
        }
    }
    @TargetApi (Build.VERSION_CODES.O)
    private fun crearCanal() {
        val notificacionCanal = NotificationChannel(ID,NOMBRE, NotificationManager.IMPORTANCE_DEFAULT)
        notificacionCanal.enableLights(true)
        notificacionCanal.enableVibration(true)
        notificacionCanal.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        getManager().createNotificationChannel(notificacionCanal)
    }

    private fun getManager(): NotificationManager {
        if(notificacionManager == null){
            notificacionManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return notificacionManager as NotificationManager
    }

    @TargetApi(Build.VERSION_CODES.O)
    public fun getNotifications(titulo: String, cuerpo: String, pIntent: PendingIntent, sonidoUri: Uri, icono: String): Notification.Builder{
        return Notification.Builder(applicationContext, ID).setContentIntent(pIntent).setContentTitle(titulo).setContentText(cuerpo).setSound(sonidoUri).setAutoCancel(true).setSmallIcon(icono.toInt())
    }
}