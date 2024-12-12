package com.tencent.devops.remotedev.pojo.event

data class RemoteDevUpdateEvent(
    val userId: String,
    val workspaceName: String,
    val type: UpdateEventType,
    var status: Boolean,
    var environmentUid: String? = null,
    /*带区域的ip*/
    var environmentHost: String? = null,
    /*不带区域的ip*/
    var environmentIp: String? = null,
    var resourceId: String? = null,
    var macAddress: String? = null,
    var errorMsg: String? = null,
    val taskUid: String? = null
)
