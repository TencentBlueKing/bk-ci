package com.tencent.devops.common.wechatwork.model.robot

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.wechatwork.model.enums.MsgType

data class RobotTextSendMsg(
    @JsonProperty("msgtype")
    val msgType: String = MsgType.text.name,
    @JsonProperty("chatid")
    val chatId: String,
    val text: TextMsg
)
