package com.tencent.devops.common.wechatwork.model.enums

enum class MsgType(private val type: String) {
    text("text"),
    image("image"),
    vocie("vocie"),
    file("file"),
    emotion("emotion"),
    forward("forward"),
    Event("Event")
}