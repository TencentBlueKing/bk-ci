package com.tencent.devops.remotedev.pojo

enum class OpHistoryCopyWriting(val default: String) {
    CREATE("基于%s的%s分支创建了一个%s的开发环境"),
    FIRST_START("激活了本环境"), // 1
    NOT_FIRST_START("重新激活了本环境"), // 1
    TIMEOUT_SLEEP("此环境已处于\"不活跃\"状态30分钟，即将进入休眠"), // 2
    DELETE("删除了本环境"), // 3
    ACTION_CHANGE("状态变更: %s -> %s") // 4
}
