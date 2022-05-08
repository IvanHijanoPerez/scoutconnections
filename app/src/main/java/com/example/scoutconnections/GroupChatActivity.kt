package com.example.scoutconnections

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class GroupChatActivity : AppCompatActivity() {

    lateinit var groupId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)

        groupId = intent.getStringExtra("groupId").toString()
    }
}