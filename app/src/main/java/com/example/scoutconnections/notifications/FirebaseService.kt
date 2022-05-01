package com.example.scoutconnections.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.core.app.NotificationCompat
import com.example.scoutconnections.ChatActivity

import com.google.firebase.database.FirebaseDatabase


class FirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val user = FirebaseAuth.getInstance().currentUser
        if(user != null){
            val ref = FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").getReference("Tokens")
            val tokenN = Token(token)
            ref.child(user!!.uid).setValue(tokenN)
        }

    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val sp = getSharedPreferences("SP_USER", MODE_PRIVATE)
        val actualSavedUser = sp.getString("Current_USERID", "None")

        val sent = message.data["sent"]
        val user = message.data["user"]

        val fUser = FirebaseAuth.getInstance().currentUser
        if(fUser != null && sent.equals(fUser.uid)){
            if(!actualSavedUser.equals(user)){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    sendOAndUpperNotification(message)
                }else{
                    sendNormalNotification(message)
                }
            }
        }
    }

    private fun sendNormalNotification(message: RemoteMessage) {
        val user = message.data["user"]
        val icon = message.data["icon"]
        val title = message.data["title"]
        val body = message.data["body"]

        val notification = message.notification
        val i = Integer.parseInt(user?.replace("[\\D]".toRegex(), ""))
        val intent = Intent(this,ChatActivity::class.java)
        val bundle = Bundle()
        bundle.putString("uidUser", user)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pIntent = i?.let {
            PendingIntent.getActivity(this,
                it, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        val defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = icon?.let { NotificationCompat.Builder(this).setSmallIcon(it.toInt()).setContentText(body).setContentTitle(title).setAutoCancel(true).setSound(defSoundUri).setContentIntent(pIntent) }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var j = 0
        if(i!! >0){
            j=i
        }
        notificationManager.notify(j, builder?.build())
    }

    private fun sendOAndUpperNotification(message: RemoteMessage) {
        val user = message.data["user"]
        val icon = message.data["icon"]
        val title = message.data["title"]
        val body = message.data["body"]

        val notification = message.notification
        val i = Integer.parseInt(user?.replace("[\\D]".toRegex(), ""))
        val intent = Intent(this,ChatActivity::class.java)
        val bundle = Bundle()
        bundle.putString("uidUser", user)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pIntent = i?.let {
            PendingIntent.getActivity(this,
                it, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        val defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notif = NotificationOreoUpper(this)
        val builder = notif.getNotifications(title!!, body!!, pIntent!!, defSoundUri, icon!!)

        var j = 0
        if(i!! >0){
            j=i
        }
        notif.notificationManager?.notify(j, builder?.build())
    }

}