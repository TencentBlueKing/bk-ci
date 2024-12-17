package com.tencent.devops.remotedev.pojo

enum class OpHistoryCopyWriting(
    val default: String
) {
    CREATE_WINDOWS("opHistoryCopyWriting.createWindows"), // 创建了一个云桌面环境
    FIRST_START("opHistoryCopyWriting.firstStart"), // 激活了本环境
    NOT_FIRST_START("opHistoryCopyWriting.notFirstStart"), // 重新激活了本环境
    SAFE_INITIALIZATION("opHistoryCopyWriting.safeInitialization"), // 此环境正在安全初始化
    MANUAL_STOP("opHistoryCopyWriting.manualStop"), // 主动关闭了工作空间
    DELETE("opHistoryCopyWriting.delete"), // 删除了本环境
    SHARE("opHistoryCopyWriting.share"), // 给%s共享了此环境
    ACTION_CHANGE("opHistoryCopyWriting.actionChange") // 状态变更: %s -> %s
}
