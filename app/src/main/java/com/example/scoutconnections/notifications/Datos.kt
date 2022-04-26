package com.example.scoutconnections.notifications

class Datos {
    var usuario: String? = null
    var cuerpo: String? = null
    var titulo: String? = null
    var enviado: String? = null
    var icono: Int? = null

    constructor(){}
    constructor(
        usuario: String?,
        cuerpo: String?,
        titulo: String?,
        enviado: String?,
        icono: Int?
    ) {
        this.usuario = usuario
        this.cuerpo = cuerpo
        this.titulo = titulo
        this.enviado = enviado
        this.icono = icono
    }
}