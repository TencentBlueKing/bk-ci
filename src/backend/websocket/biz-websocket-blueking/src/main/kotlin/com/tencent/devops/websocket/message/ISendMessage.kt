package com.tencent.devops.websocket.message

import com.tencent.devops.common.websocket.dispatch.message.MqMessage
import com.tencent.devops.common.websocket.pojo.MessageInfo

interface ISendMessage {
    fun sendWebsocketMessage(messageInfo: MessageInfo)

    fun buildMessageInfo(event: MqMessage): MessageInfo?
}