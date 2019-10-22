package com.tencent.devops.process.pojo

/**
 * @ Author     ：Royal Huang
 * @ Date       ：Created in 16:07 2019-09-19
 */

enum class ErrorType(val statusName: String, val visiable: Boolean) {
    SYSTEM("系统运行错误", true), // 0 平台运行报错
    USER("业务逻辑错误", true), // 1 任务执行报错
}