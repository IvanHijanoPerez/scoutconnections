package com.example.scoutconnections.models

class UserModel {
    var name: String? = null
    var email: String? = null
    var cover: String? = null
    var image: String? = null
    var monitor: Boolean? = null
    var phone: String? = null
    var uid: String? = null
    var status: String? = null
    var typingTo: String? = null


    constructor() {}
    constructor(
        name: String?,
        email: String?,
        cover: String?,
        image: String?,
        monitor: Boolean?,
        phone: String?,
        uid: String?,
        status: String?,
        typingTo: String?
    ) {
        this.name = name
        this.email = email
        this.cover = cover
        this.image = image
        this.monitor = monitor
        this.phone = phone
        this.uid = uid
        this.status = status
        this.typingTo = typingTo
    }
}