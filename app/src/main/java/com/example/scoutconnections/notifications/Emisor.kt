package com.example.scoutconnections.notifications

class Emisor {
    var data: Datos? = null
    var to: String? = null
    constructor(){}
    constructor(data: Datos?, to: String?) {
        this.data = data
        this.to = to
    }
}