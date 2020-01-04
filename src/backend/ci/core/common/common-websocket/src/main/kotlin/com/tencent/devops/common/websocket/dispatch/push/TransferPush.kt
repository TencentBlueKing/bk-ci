package com.tencent.devops.common.websocket.dispatch.push

abstract class TransferPush (
    open val eventName: String?,
    open val userId: String,
    open var page: String?,
    open var delayMills: Int? = 0,
    open val transferData: Map<String, Any>
)