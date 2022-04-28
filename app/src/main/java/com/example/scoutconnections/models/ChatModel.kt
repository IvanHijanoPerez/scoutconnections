package com.example.scoutconnections.models

class ChatModel {
    var message: String? = null
    var sender: String? = null
    var receiver: String? = null
    var time: String? = null
    var seen: Boolean? = null

    constructor() {}

    constructor(
        message: String?,
        sender: String?,
        receiver: String?,
        time: String?,
        seen: Boolean?
    ) {
        this.message = message
        this.sender = sender
        this.receiver = receiver
        this.time = time
        this.seen = seen
    }
}