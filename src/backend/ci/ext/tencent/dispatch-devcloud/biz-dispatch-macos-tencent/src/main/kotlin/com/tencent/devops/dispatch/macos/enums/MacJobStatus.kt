package com.tencent.devops.dispatch.macos.enums

enum class MacJobStatus(val title: String) {
    Running("running"),//执行中
    Done("done"),//完成
    Failure("failure"),//失败
    ShutDownError("shutDownerror");//关闭失败
}
