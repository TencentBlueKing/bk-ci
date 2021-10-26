package com.tencent.devops.common.wechatwork.model.robot

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.wechatwork.model.enums.MsgType

data class RobotMarkdownSendMsg(
    @JsonProperty("msgtype")
    val msgType: String = MsgType.markdown.name,
    @JsonProperty("chatid")
    val chatId: String,
    val markdown: MsgInfo
)
