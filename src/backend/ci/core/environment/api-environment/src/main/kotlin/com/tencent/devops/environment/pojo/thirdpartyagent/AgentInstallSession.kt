package com.tencent.devops.environment.pojo.thirdpartyagent

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.environment.pojo.NodeTagAddOrDeleteTagItem
import com.tencent.devops.environment.pojo.enums.AgentType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "构建机安装会话模式")
enum class AgentInstallMode {
    FIRST_IMPORT,
    REINSTALL
}

@Schema(title = "构建机安装会话配置")
class AgentInstallSessionRequest(
    @get:Schema(title = "安装会话模式", required = true)
    val mode: AgentInstallMode,
    @get:Schema(title = "操作系统", required = true)
    val os: OS,
    @get:Schema(title = "网关地区")
    val zone: String?,
    @get:Schema(title = "登录账户名")
    val loginName: String? = null,
    @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @get:Schema(title = "登录账户密码", accessMode = Schema.AccessMode.WRITE_ONLY)
    val loginPassword: String? = null,
    @get:Schema(title = "Agent安装模式")
    val installType: TPAInstallType? = null,
    @get:Schema(title = "第三方构建机节点类型")
    val agentType: AgentType? = null,
    @get:Schema(title = "构建并发数，0表示无限制", required = true, minimum = "0")
    val parallelTaskCount: Int,
    @get:Schema(title = "初始节点标签")
    val tags: List<NodeTagAddOrDeleteTagItem> = emptyList(),
    @get:Schema(title = "重装目标Agent Hash ID")
    val targetAgentId: String? = null
) {
    override fun toString(): String =
        "AgentInstallSessionRequest(mode=$mode, os=$os, zone=$zone, loginName=$loginName, " +
            "loginPassword=***, installType=$installType, agentType=$agentType, " +
            "parallelTaskCount=$parallelTaskCount, tags=$tags, targetAgentId=$targetAgentId)"
}

@Schema(title = "安装会话标签快照")
data class AgentInstallTagSnapshot(
    @get:Schema(title = "标签键ID", required = true)
    val tagKeyId: Long,
    @get:Schema(title = "标签键名称", required = true)
    val tagKeyName: String,
    @get:Schema(title = "标签值ID", required = true)
    val tagValueId: Long,
    @get:Schema(title = "标签值名称", required = true)
    val tagValueName: String
)

@Schema(title = "安装会话环境预览项")
data class AgentInstallEnvironmentInfo(
    @get:Schema(title = "环境Hash ID", required = true)
    val envId: String,
    @get:Schema(title = "环境名称", required = true)
    val envName: String
)

@Schema(title = "安装会话环境变更预览")
data class AgentInstallEnvironmentPreview(
    @get:Schema(title = "当前已关联环境")
    val associated: List<AgentInstallEnvironmentInfo> = emptyList(),
    @get:Schema(title = "接入后将加入的环境")
    val willJoin: List<AgentInstallEnvironmentInfo> = emptyList(),
    @get:Schema(title = "接入后将移除的环境")
    val willLeave: List<AgentInstallEnvironmentInfo> = emptyList(),
    @get:Schema(title = "需在构建机接入后确认的环境")
    val pending: List<AgentInstallEnvironmentInfo> = emptyList()
)

@Schema(title = "安装会话预览")
data class AgentInstallSessionPreview(
    @get:Schema(title = "安装会话模式", required = true)
    val mode: AgentInstallMode,
    @get:Schema(title = "操作系统", required = true)
    val os: OS,
    @get:Schema(title = "网关地区")
    val zone: String?,
    @get:Schema(title = "登录账户名")
    val loginName: String?,
    @get:Schema(title = "是否已配置登录密码", required = true)
    val loginPasswordConfigured: Boolean,
    @get:Schema(title = "Agent安装模式")
    val installType: TPAInstallType?,
    @get:Schema(title = "第三方构建机节点类型", required = true)
    val agentType: AgentType,
    @get:Schema(title = "构建并发数，0表示无限制", required = true)
    val parallelTaskCount: Int,
    @get:Schema(title = "标签快照")
    val tags: List<AgentInstallTagSnapshot>,
    @get:Schema(title = "重装目标Agent Hash ID")
    val targetAgentId: String?,
    @get:Schema(title = "环境变更预览")
    val environments: AgentInstallEnvironmentPreview
)

@Schema(title = "安装会话节点结果汇总")
data class AgentInstallSessionSummary(
    @get:Schema(title = "总节点数", required = true)
    val total: Int,
    @get:Schema(title = "待处理节点数", required = true)
    val pending: Int,
    @get:Schema(title = "安装中节点数", required = true)
    val installing: Int,
    @get:Schema(title = "导入中节点数", required = true)
    val importing: Int,
    @get:Schema(title = "成功节点数", required = true)
    val succeeded: Int,
    @get:Schema(title = "失败节点数", required = true)
    val failed: Int
) {
    companion object {
        val EMPTY = AgentInstallSessionSummary(0, 0, 0, 0, 0, 0)
    }
}

@Schema(title = "创建构建机安装会话结果")
data class AgentInstallSessionCreateResponse(
    @get:Schema(title = "安装会话ID", required = true)
    val sessionId: String,
    @get:Schema(title = "安装命令", required = true)
    val command: String,
    @get:Schema(title = "失效时间", required = true)
    val expiredAt: LocalDateTime,
    @get:Schema(title = "是否复用了未过期会话", required = true)
    val reused: Boolean,
    @get:Schema(title = "会话不可变配置与环境预览", required = true)
    val preview: AgentInstallSessionPreview,
    @get:Schema(title = "节点结果汇总", required = true)
    val summary: AgentInstallSessionSummary
)

@Schema(title = "构建机安装会话详情")
data class AgentInstallSessionDetail(
    @get:Schema(title = "安装会话ID", required = true)
    val sessionId: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "创建人", required = true)
    val createdBy: String,
    @get:Schema(title = "会话状态", required = true)
    val status: String,
    @get:Schema(title = "安装命令", required = true)
    val command: String,
    @get:Schema(title = "失效时间", required = true)
    val expiredAt: LocalDateTime,
    @get:Schema(title = "创建时间", required = true)
    val createdAt: LocalDateTime,
    @get:Schema(title = "来源会话ID")
    val previousSessionId: String?,
    @get:Schema(title = "会话不可变配置与环境预览", required = true)
    val preview: AgentInstallSessionPreview,
    @get:Schema(title = "节点结果汇总", required = true)
    val summary: AgentInstallSessionSummary
)

@Schema(title = "安装会话节点结果")
data class AgentInstallSessionNodeInfo(
    @get:Schema(title = "Agent Hash ID", required = true)
    val agentId: String,
    @get:Schema(title = "Node Hash ID")
    val nodeId: String?,
    @get:Schema(title = "节点状态", required = true)
    val status: String,
    @get:Schema(title = "主机名")
    val hostname: String?,
    @get:Schema(title = "IP地址")
    val ip: String?,
    @get:Schema(title = "失败原因")
    val errorMessage: String?,
    @get:Schema(title = "开始时间")
    val startedAt: LocalDateTime?,
    @get:Schema(title = "结束时间")
    val finishedAt: LocalDateTime?,
    @get:Schema(title = "Agent版本")
    val agentVersion: String?
)

@Schema(title = "构建机重装上下文")
data class AgentReinstallContext(
    @get:Schema(title = "Agent Hash ID", required = true)
    val agentId: String,
    @get:Schema(title = "节点名称", required = true)
    val displayName: String,
    @get:Schema(title = "操作系统", required = true)
    val os: OS,
    @get:Schema(title = "网关地区")
    val zone: String?,
    @get:Schema(title = "Agent安装模式")
    val installType: TPAInstallType?,
    @get:Schema(title = "第三方构建机节点类型", required = true)
    val agentType: AgentType,
    @get:Schema(title = "当前构建并发数，0表示无限制", required = true)
    val parallelTaskCount: Int,
    @get:Schema(title = "当前节点标签")
    val tags: List<AgentInstallTagSnapshot>,
    @get:Schema(title = "当前配置的环境预览", required = true)
    val preview: AgentInstallEnvironmentPreview,
    @get:Schema(title = "是否允许重装", required = true)
    val canReinstall: Boolean,
    @get:Schema(title = "禁止重装原因")
    val denyReason: String?
)
