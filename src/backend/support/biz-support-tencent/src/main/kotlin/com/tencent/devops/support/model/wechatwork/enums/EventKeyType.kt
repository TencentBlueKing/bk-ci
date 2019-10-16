package com.tencent.devops.support.model.wechatwork.enums

enum class EventKeyType(val reg: Regex) {
    PROJECT_PIPELINE_LIST("^\\w+:project:\\w+:pipeline:list\$".toRegex()),
    PROJECT("^\\w+:project\$".toRegex()),
    PROJECT_PIPELINE_GET("^\\w+:project:\\w+:pipeline:[^:]*:pipelineName:[^:]*:get\$".toRegex()),
    PROJECT_PIPELINE_START("^\\w+:project:\\w+:pipeline:[^:]*:pipelineName:[^:]*:start\$".toRegex()),
    PROJECT_PIPELINE_STOP("^\\w+:project:\\w+:pipeline:[^:]*:pipelineName:[^:]*:stop\$".toRegex()),
    SERVICE_HUMAN("^\\w+:service:human\$".toRegex())
}
