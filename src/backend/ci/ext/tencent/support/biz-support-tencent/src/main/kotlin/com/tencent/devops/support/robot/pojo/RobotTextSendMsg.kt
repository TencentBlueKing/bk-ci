package com.tencent.devops.support.robot.pojo

import com.tencent.devops.support.model.wechatwork.enums.MsgType

data class RobotTextSendMsg(
    val msgType: String = MsgType.text.name,
    val chatId: String,
    val text: TextMsg
)
