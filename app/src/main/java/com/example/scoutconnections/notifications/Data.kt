package com.example.scoutconnections.notifications

class Data {
    var user: String? = null
    var body: String? = null
    var title: String? = null
    var sent: String? = null
    var icon: Int? = null

    constructor(){}
    constructor(
        user: String?,
        body: String?,
        title: String?,
        sent: String?,
        icon: Int?
    ) {
        this.user = user
        this.body = body
        this.title = title
        this.sent = sent
        this.icon = icon
    }
}