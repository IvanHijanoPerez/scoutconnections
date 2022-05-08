package com.example.scoutconnections.models

class GroupModel {
    var gid: String? = null
    var creator: String? = null
    var title: String? = null
    var description: String? = null
    var image: String? = null
    var time: String? = null

    constructor() {}

    constructor(
        gid: String?,
        creator: String?,
        title: String?,
        description: String?,
        image: String?,
        time: String?
    ) {
        this.gid = gid
        this.creator = creator
        this.title = title
        this.description = description
        this.image = image
        this.time = time
    }
}