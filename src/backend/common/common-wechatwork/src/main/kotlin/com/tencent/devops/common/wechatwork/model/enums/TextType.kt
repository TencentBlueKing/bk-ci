package com.tencent.devops.common.wechatwork.model.enums

enum class TextType(val reg: Regex) {
    GROUP_ID("群ID".toRegex()),
    PROJECT("^project\$".toRegex()),
    PROJECT_PIPELINE_GET("^project:\\w+:pipeline:\\w+:get\$".toRegex()),
    PROJECT_PIPELINE_START("^project:\\w+:pipeline:\\w+:start\$".toRegex()),
    PROJECT_PIPELINE_STOP("^project:\\w+:pipeline:\\w+:stop\$".toRegex()),
    SERVICE_HUMAN("^service:human\$".toRegex())
}
