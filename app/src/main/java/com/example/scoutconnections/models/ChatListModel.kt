package com.example.scoutconnections.models

class ChatListModel {
    var uid: String? = null

    constructor() {}

    constructor(
        message: String?
    ) {
        this.uid = message
    }
}