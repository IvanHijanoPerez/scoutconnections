package com.example.scoutconnections.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.annotation.NonNull
import androidx.core.app.NotificationCompat
import com.example.scoutconnections.ChatActivity

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.ktx.remoteMessage


class ServicioFirebase : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val usuario = FirebaseAuth.getInstance().currentUser
        if(usuario != null){
            val ref = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Tokens")
            val tokenN = Token(token)
            ref.child(usuario!!.uid).setValue(tokenN)
        }

    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val sp = getSharedPreferences("SP_USER", MODE_PRIVATE)
        val usuarioActualGuardado = sp.getString("Current_USERID", "None")

        val enviado = message.data.get("enviado")
        val usuario = message.data.get("usuario")
        val fUsuario = FirebaseAuth.getInstance().currentUser
        if(fUsuario != null && enviado.equals(fUsuario.uid)){
            if(!usuarioActualGuardado.equals(usuario)){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    enviarOyEncimaNotificacion(message)
                }else{
                    enviarNormalNotificacion(message)
                }
            }
        }
    }

    private fun enviarNormalNotificacion(message: RemoteMessage) {
        val usuario = message.data.get("usuario")
        val icono = message.data.get("icono")
        val titulo = message.data.get("titulo")
        val cuerpo = message.data.get("cuerpo")

        val notificacion = message.notification
        val i = Integer.parseInt(usuario?.replace("[\\D]".toRegex(), ""))
        val intent = Intent(this,ChatActivity::class.java)
        val bundle = Bundle()
        bundle.putString("uidUsuario", usuario)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pIntent = i?.let {
            PendingIntent.getActivity(this,
                it, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        val defSonidoUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = icono?.let { NotificationCompat.Builder(this).setSmallIcon(it.toInt()).setContentText(cuerpo).setContentTitle(titulo).setAutoCancel(true).setSound(defSonidoUri).setContentIntent(pIntent) }

        val notificacionManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var j = 0
        if(i!! >0){
            j=i
        }
        notificacionManager.notify(j, builder?.build())
    }

    private fun enviarOyEncimaNotificacion(message: RemoteMessage) {
        val usuario = message.data.get("usuario")
        val icono = message.data.get("icono")
        val titulo = message.data.get("titulo")
        val cuerpo = message.data.get("cuerpo")

        val notificacion = message.notification
        val i = Integer.parseInt(usuario?.replace("[\\D]".toRegex(), ""))
        val intent = Intent(this,ChatActivity::class.java)
        val bundle = Bundle()
        bundle.putString("uidUsuario", usuario)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pIntent = i?.let {
            PendingIntent.getActivity(this,
                it, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        val defSonidoUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notif = NotificacionOreoEncima(this)
        val builder = notif.getNotifications(titulo!!, cuerpo!!, pIntent!!, defSonidoUri, icono!!)

        var j = 0
        if(i!! >0){
            j=i
        }
        notif.notificacionManager?.notify(j, builder?.build())
    }

}