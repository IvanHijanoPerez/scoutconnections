package com.example.scoutconnections.models

class ChatModel {
    var mensaje: String? = null
    var emisor: String? = null
    var receptor: String? = null
    var tiempo: String? = null
    var leido: Boolean? = null

    constructor(){}

    constructor(
        mensaje: String?,
        emisor: String?,
        receptor: String?,
        tiempo: String?,
        leido: Boolean?
    ) {
        this.mensaje = mensaje
        this.emisor = emisor
        this.receptor = receptor
        this.tiempo = tiempo
        this.leido = leido
    }
}