package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

data class SupRecordInfo(
    @get:Schema(title = "出口IP")
    val requestIp: String?,
    @get:Schema(title = "项目管理员")
    val projectManager: Set<String>?,
    @get:Schema(title = "客户端版本")
    val clientVersion: String?,
    @get:Schema(title = "机器状态")
    val machineStatus: String?,
    @get:Schema(title = "CDS版本")
    val cdsVersion: String?,
    @get:Schema(title = "CDS区域")
    val cdsRegion: String?,
    @get:Schema(
        title = "CDS状态, 1 正常, 2 关机, 0 未知异常, -1 磁盘I/O使用率, -2 Agent心跳丢失, -3 CPU总使用率过高, " +
                "-4 主机重启, -5 磁盘使用率过高, -6 Ping不可达, 其它<0错误码 未知异常"
    )
    val cdsStatus: String?,
    @get:Schema(title = "CDS端口号")
    val cdsPort: String?,
    @get:Schema(title = "agent状态, NOT_ALIVE = 0, ALIVE = 1, TERMINATED = 2, NOT_INSTALLED = 3")
    val agentStatus: String?,
    @get:Schema(title = "拥有者")
    val owner: String?,
    @get:Schema(title = "共享人名单")
    val viewers: Set<String>?
)
