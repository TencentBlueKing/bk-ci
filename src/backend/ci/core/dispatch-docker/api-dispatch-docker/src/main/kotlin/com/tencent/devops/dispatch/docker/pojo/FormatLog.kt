package com.tencent.devops.dispatch.docker.pojo

data class FormatLog(
    val logType: LogType,
    val washTime: String,
    val logMessageMap: Map<String, String>
)

enum class LogType {
    // 公共构建机容器负载日志
    DOCKERHOST_CONTAINER_LOAD
}
