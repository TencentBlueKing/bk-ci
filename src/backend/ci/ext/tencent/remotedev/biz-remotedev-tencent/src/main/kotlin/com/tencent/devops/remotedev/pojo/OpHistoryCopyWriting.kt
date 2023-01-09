package com.tencent.devops.remotedev.pojo

enum class OpHistoryCopyWriting(val default: String) {
    CREATE("基于%s的%s分支创建了一个开发环境"),
    FIRST_START("激活了本环境"),
    NOT_FIRST_START("重新激活了本环境"),
    TIMEOUT_SLEEP("此环境已处于\"不活跃\"状态30分钟，即将进入休眠"),
    TIMEOUT_STOP("此环境已处于\"不活跃\"状态7天，即将进行销毁"),
    MANUAL_STOP("主动关闭了工作空间"),
    DELETE("删除了本环境"),
    SHARE("给%s共享了此环境"),
    ACTION_CHANGE("状态变更: %s -> %s")
}
