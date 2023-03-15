package com.tencent.devops.dispatch.macos.enums

enum class MacJobStatus(val title: String) {
    Running("执行中"),
    Done("完成"),
    Failure("失败"),
    ShutDownError("关闭失败");
}
