package com.tencent.devops.common.wechatwork.model.enums

enum class UploadMediaType(private val type: String) {
    image("image"),
    voice("voice"),
    video("video"),
    file("file")
}