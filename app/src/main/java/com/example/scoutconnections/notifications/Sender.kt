package com.example.scoutconnections.notifications

class Sender {
    var data: Data? = null
    var to: String? = null
    constructor(){}
    constructor(data: Data?, to: String?) {
        this.data = data
        this.to = to
    }
}