package com.tencent.devops.websocket.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.websocket.dispatch.push.TransferPush

@Event(exchange = MQ.EXCHANGE_WEBSOCKET_TRANSFER_FANOUT, routeKey = MQ.ROUTE_WEBSOCKET_TRANSFER_EVENT)
class ChangePageTransferEvent(
    override val eventName: String ?= "changePageEvent",
    override val userId: String,
    override var page: String?,
    override var delayMills: Int?=0,
    override val transferData: Map<String, Any>
): TransferPush(eventName, userId, page,delayMills, transferData)