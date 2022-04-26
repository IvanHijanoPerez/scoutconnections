package com.example.scoutconnections.models

class UsuarioModel {
    var nombre: String? = null
    var correo: String? = null
    var fondo: String? = null
    var imagen: String? = null
    var monitor: Boolean? = null
    var telefono: String? = null
    var uid: String? = null

    constructor() {}
    constructor(
        nombre: String?,
        correo: String?,
        fondo: String?,
        imagen: String?,
        monitor: Boolean?,
        telefono: String?,
        uid: String?
    ) {
        this.nombre = nombre
        this.correo = correo
        this.fondo = fondo
        this.imagen = imagen
        this.monitor = monitor
        this.telefono = telefono
        this.uid = uid
    }
}