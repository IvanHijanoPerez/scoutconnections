package com.example.scoutconnections.models

class GroupChatModel {
    var message: String? = null
    var sender: String? = null
    var time: String? = null
    var type: String? = null

    constructor() {}

    constructor(
        message: String?,
        sender: String?,
        time: String?,
        type: String?
    ) {
        this.message = message
        this.sender = sender
        this.time = time
        this.type = type
    }
}