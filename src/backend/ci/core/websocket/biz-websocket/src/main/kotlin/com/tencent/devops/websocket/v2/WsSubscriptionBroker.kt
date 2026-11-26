package com.tencent.devops.websocket.v2

import com.tencent.devops.common.websocket.v2.WsInvalidation

interface WsSubscriptionBroker {
    val ready: Boolean
    fun bindingCount(): Int = 0
    fun bind(keys: Set<String>)
    fun unbind(keys: Set<String>)
    fun listen(onMessage: (String, WsInvalidation) -> Unit, onReset: () -> Unit)
}
