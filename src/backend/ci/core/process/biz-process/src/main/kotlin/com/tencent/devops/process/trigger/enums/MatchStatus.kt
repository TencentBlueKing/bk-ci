package com.tencent.devops.process.trigger.enums

enum class MatchStatus {
    // 匹配成功
    SUCCESS,

    // 插件不匹配
    ELEMENT_NOT_MATCH,

    // 代码库不匹配
    REPOSITORY_NOT_MATCH,

    // 事件类型不匹配
    EVENT_TYPE_NOT_MATCH,

    // 条件不匹配
    CONDITION_NOT_MATCH;
}
