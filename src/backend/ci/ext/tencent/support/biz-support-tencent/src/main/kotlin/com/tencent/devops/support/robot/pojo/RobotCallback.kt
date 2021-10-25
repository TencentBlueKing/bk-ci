package com.tencent.devops.support.robot.pojo

data class RobotCallback (
    val webhookUrl: String,
    val userId: String,
    val name: String,
    val msgType: String,
    val content: String,
    val msgId: String,
    val chatId: String,
    val getChatInfoUrl: String
)
