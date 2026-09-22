package com.tencent.devops.process.trigger.enums

enum class MatchStatus {
    // 匹配成功
    SUCCESS,

    // 跳过：本插件非该事件目标（插件类型/代码库/事件类型/制品形态等不匹配），无需记录触发事件，直接匹配下一个插件
    SKIP,

    // 条件不匹配：记录触发事件失败原因
    CONDITION_NOT_MATCH;
}
