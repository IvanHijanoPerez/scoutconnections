package com.example.scoutconnections.models

class CommentModel {
    var cid: String? = null
    var comment: String? = null
    var time: String? = null
    var creator: String? = null

    constructor() {}

    constructor(
        cid: String?,
        comment: String?,
        time: String?,
        creator: String?
    ) {
        this.cid = cid
        this.comment = comment
        this.time = time
        this.creator = creator
    }
}