package com.example.scoutconnections

import android.app.Application
import android.content.Context
import com.google.firebase.database.FirebaseDatabase

open class Scouts : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance("https://scout-connections-default-rtdb.europe-west1.firebasedatabase.app").setPersistenceEnabled(true)
        mInstance = this
    }

    companion object {
        lateinit var mInstance: Scouts
        fun getContext(): Context? {
            return mInstance.applicationContext
        }
    }
}