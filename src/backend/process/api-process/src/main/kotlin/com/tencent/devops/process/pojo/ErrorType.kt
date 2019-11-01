package com.tencent.devops.process.pojo

enum class ErrorType(val statusName: String, val visiable: Boolean) {
    SYSTEM("系统运行错误", true), // 0 平台运行报错
    USER("业务逻辑错误", true), // 1 任务执行报错
}