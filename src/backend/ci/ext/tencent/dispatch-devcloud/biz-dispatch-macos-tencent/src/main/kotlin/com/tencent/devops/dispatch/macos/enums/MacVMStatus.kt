package com.tencent.devops.dispatch.macos.enums

enum class MacVMStatus(val title: String) {
    Unknown("未知"), // 初始，未知状态
    Idle("空闲"), // 空闲
    BeUsed("使用中"), // 使用中
    shutDown("已关闭"), // 已关闭
    Updating("升级中"), // 升级中
    Exception("异常") // 异常
}
