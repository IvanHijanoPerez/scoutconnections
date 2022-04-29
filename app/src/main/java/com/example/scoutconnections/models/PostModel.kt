package com.example.scoutconnections.models

class PostModel {
    var pid: String? = null
    var title: String? = null
    var description: String? = null
    var image: String? = null
    var time: String? = null
    var creator: String? = null


    constructor() {}
    constructor(
        pid: String?,
        title: String?,
        description: String?,
        image: String?,
        time: String?,
        creator: String?
    ) {
        this.pid = pid
        this.title = title
        this.description = description
        this.image = image
        this.time = time
        this.creator = creator
    }
}