package com.example.scoutconnections

import android.app.Application
import android.content.Context

open class Scouts : Application() {
    override fun onCreate() {
        super.onCreate()
        mInstance = this
    }

    companion object {
        lateinit var mInstance: Scouts
        fun getContext(): Context? {
            return mInstance.applicationContext
        }
    }
}