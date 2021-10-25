package com.tencent.devops.support.robot.pojo

data class RobotSendMsg(
    val MsgType: String,
    val Text: String,
    val Content: String,
    val MentionedList: String
)
