package com.tencent.devops.dispatch.windows.enums

enum class WindowsJobStatus (val title: String) {
    Running("执行中"),
    Done("完成"),
    Failure("失败"),
    ShutDownError("关闭失败");
}
