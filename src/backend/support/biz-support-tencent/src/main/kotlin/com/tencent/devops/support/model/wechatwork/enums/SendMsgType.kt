package com.tencent.devops.support.model.wechatwork.enums

enum class SendMsgType(private val type: String) {
    text("text"),
    image("image"),
    file("file"),
    rich_text("rich_text")
}