package com.tencent.devops.common.websocket.enum


enum class NotityLevel(
    private val level: Int,
    private val leavelMessage: String
) {
        RIGHT_LEVEL(0, "正常消息"),
        HIGH_LEVEL(1, "最高等级，通知不会主动关闭，必须用户操作才能关闭"),
        LOW_LEVEL(9, "普通的通知提醒，会自动关闭");

}