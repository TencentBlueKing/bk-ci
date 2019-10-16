package com.tencent.devops.common.wechatwork.model.enums

enum class EventKeyType(val reg: Regex) {
    PROJECT_PIPELINE_LIST("^\\w+:project:\\w+:pipeline:list\$".toRegex()),
    PROJECT("^\\w+:project\$".toRegex()),
    PROJECT_PIPELINE_GET("^\\w+:project:\\w+:pipeline:\\w+:get\$".toRegex()),
    PROJECT_PIPELINE_START("\\w+:^project:\\w+:pipeline:\\w+:start\$".toRegex()),
    PROJECT_PIPELINE_STOP("^\\w+:project:\\w+:pipeline:\\w+:stop\$".toRegex()),
    SERVICE_HUMAN("^\\w+:service:human\$".toRegex())
}
