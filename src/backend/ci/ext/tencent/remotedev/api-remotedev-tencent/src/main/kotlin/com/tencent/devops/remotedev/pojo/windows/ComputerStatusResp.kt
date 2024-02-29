package com.tencent.devops.remotedev.pojo.windows

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Parameter

@Schema(title = "windows 机器状态信息")
data class ComputerStatusResp(
    @Parameter(description = "机器总数")
    val count: Int,
    @Parameter(description = "机器状态信息")
    val status: List<ComputerStatusData>,
    @Parameter(description = "机器登录信息")
    val users: List<ComputerUserData>
)

@Schema(title = "机器状态信息")
data class ComputerStatusData(
    var value: Int,
    val type: ComputerStatusEnum,
    val msg: String
)

enum class ComputerStatusEnum(val status: Int, val message: String) {
    NORMAL(1, "正常"),
    DISK_IO_ERROR(-1, "磁盘IO异常"),
    AGENT_HEART_LOSE(-2, "Agent心跳丢失"),
    CPU_HIGH(-3, "CPU使用率过高"),
    RESTART(-4, "主机重启"),
    DISK_HIGH(-5, "磁盘使用率过高"),
    PING_NOT_FOUND(-6, "Ping不可达"),
    UNKNOWN(-100, "未知异常");

    companion object {
        // <= 0的都是异常，然后没有定义的就是未知异常
        fun getEnumFromStatus(status: Int): ComputerStatusEnum? {
            return when (status) {
                1 -> NORMAL
                -1 -> DISK_IO_ERROR
                -2 -> AGENT_HEART_LOSE
                -3 -> CPU_HIGH
                -4 -> RESTART
                -5 -> DISK_HIGH
                -6 -> PING_NOT_FOUND
                else -> {
                    if (status < 0 || status == 0) {
                        UNKNOWN
                    } else {
                        null
                    }
                }
            }
        }
    }
}

data class ComputerUserData(
    var value: Int,
    val names: MutableMap<String, List<String>>?,
    val type: ComputerUserEnum
)

enum class ComputerUserEnum {
    LOGIN,
    LOGOUT
}
