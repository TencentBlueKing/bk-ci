package com.tencent.devops.support.robot.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.support.model.wechatwork.enums.MsgType

data class RobotTextSendMsg(
    @JsonProperty("msgtype")
    val msgType: String = MsgType.text.name,
    @JsonProperty("chatid")
    val chatId: String,
    val text: TextMsg
)
