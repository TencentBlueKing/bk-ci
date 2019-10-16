package com.tencent.devops.support.model.wechatwork.enums

enum class RichtextContentType(private val type: String) {
    text("text"),
    mentioned("mentioned"),
    link("link")
}