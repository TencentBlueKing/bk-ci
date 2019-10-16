package com.tencent.devops.support.model.wechatwork.enums

enum class MsgType(private val type: String) {
    text("text"),
    image("image"),
    vocie("vocie"),
    file("file"),
    emotion("emotion"),
    forward("forward"),
    Event("Event")
}