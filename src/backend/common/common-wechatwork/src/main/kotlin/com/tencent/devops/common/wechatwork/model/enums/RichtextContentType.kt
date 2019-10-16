package com.tencent.devops.common.wechatwork.model.enums

enum class RichtextContentType(private val type: String) {
    text("text"),
    mentioned("mentioned"),
    link("link")
}