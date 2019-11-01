package com.tencent.devops.process.util.cloudStone

data class UploadTicket constructor(
    var ticketId: Int = 0,
    var randomKey: String? = null
) {

    override fun toString(): String {
        return "UploadTicket [ticketId=$ticketId, randomKey=$randomKey]"
    }
}