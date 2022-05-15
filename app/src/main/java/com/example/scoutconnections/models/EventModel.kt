package com.example.scoutconnections.models

class EventModel {
    var eid: String? = null
    var title: String? = null
    var description: String? = null
    var time: String? = null
    var image: String? = null
    var tEvent: String? = null
    var creator: String? = null


    constructor() {}
    constructor(
        eid: String?,
        title: String?,
        description: String?,
        time: String?,
        creator: String?,
        image: String?,
        tEvent: String?

    ) {
        this.eid = eid
        this.title = title
        this.description = description
        this.tEvent = tEvent
        this.time = time
        this.image = image
        this.creator = creator

    }

}