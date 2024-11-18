package com.tencent.devops.remotedev.pojo.expert

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "专家协助工单记录")
data class SupRecordData(
    @get:Schema(title = "工单ID")
    val id: Long,
    @get:Schema(title = "求助时间")
    val createTime: LocalDateTime,
    @get:Schema(title = "求助内容")
    val content: String,
    @get:Schema(title = "出口IP")
    val requestIp: String?,
    @get:Schema(title = "云桌面IP")
    val hostIp: String,
    @get:Schema(title = "项目")
    val projectId: String,
    @get:Schema(title = "项目管理员")
    val projectManager: Set<String>?,
    @get:Schema(title = "机型")
    val machineType: String,
    @get:Schema(title = "城市")
    val city: String,
    @get:Schema(title = "客户端版本")
    val clientVersion: String?,
    @get:Schema(title = "机器状态")
    val machineStatus: String?,
    @get:Schema(title = "CDS版本")
    val cdsVersion: String?,
    @get:Schema(title = "CDS区域")
    val cdsRegion: String?,
    @get:Schema(title = "CDS状态")
    val cdsStatus: String?,
    @get:Schema(title = "CDS端口号")
    val cdsPort: String?,
    @get:Schema(title = "agent状态")
    val agentStatus: String?,
    @get:Schema(title = "拥有者")
    val owner: String?,
    @get:Schema(title = "共享人名单")
    val viewers: Set<String>?,
    @get:Schema(title = "云桌面客户端登陆用户名")
    val loginName: String?
)

data class SupRecordDataResp(
    val count: Int,
    val records: List<SupRecordData>
)