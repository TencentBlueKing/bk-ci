package com.tencent.devops.common.wechatwork.model.enums

enum class SendMsgType(private val type: String) {
    text("text"),
    image("image"),
    file("file"),
    rich_text("rich_text")
}