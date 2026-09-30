package com.tencent.devops.environment.model

import java.time.LocalDateTime

enum class AgentInstallSessionMode {
    FIRST_IMPORT,
    REINSTALL
}

enum class AgentInstallSessionStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}

enum class AgentInstallSessionNodeStatus {
    PENDING,
    INSTALLING,
    IMPORTING,
    SUCCEEDED,
    FAILED;

    fun isFinished() = this == SUCCEEDED || this == FAILED
}

/**
 * 会话创建时固化的构建机安装配置，整体以Json保存在AGENT_CONFIG字段。
 * 会话配置一经创建不再修改，新增配置项时在此追加带默认值的字段即可，无需变更表结构。
 */
data class AgentInstallSessionConfig(
    // 接入区域
    val zone: String? = null,
    // 服务网关
    val gateway: String,
    // 文件网关
    val fileGateway: String? = null,
    // 安装方式
    val installType: String,
    // 构建机类型
    val agentType: String,
    // Windows登录用户名
    val loginName: String? = null,
    // Windows登录密码AES密文
    val loginPasswordCipher: String? = null,
    // 构建并发数，0表示无限制
    val parallelTaskCount: Int,
    // Docker构建并发数，为空表示不指定
    val dockerParallelTaskCount: Int? = null
) {
    override fun toString(): String =
        "AgentInstallSessionConfig(zone=$zone, gateway=$gateway, fileGateway=$fileGateway, " +
            "installType=$installType, agentType=$agentType, loginName=$loginName, " +
            "loginPasswordCipher=***, parallelTaskCount=$parallelTaskCount, " +
            "dockerParallelTaskCount=$dockerParallelTaskCount)"
}

data class AgentInstallSession(
    val id: String,
    val projectId: String,
    val mode: AgentInstallSessionMode,
    val createdBy: String,
    val os: String,
    val config: AgentInstallSessionConfig,
    val targetAgentId: Long?,
    val targetNodeId: Long?,
    val configFingerprint: String,
    val tokenHash: String,
    val tokenCipher: String,
    val status: AgentInstallSessionStatus,
    val expiredTime: LocalDateTime,
    val previousSessionId: String?,
    val createdTime: LocalDateTime,
    val updatedTime: LocalDateTime
)

data class AgentInstallSessionTag(
    val sessionId: String,
    val tagKeyId: Long,
    val tagValueId: Long,
    val tagKeyName: String,
    val tagValueName: String
)

data class AgentInstallSessionNode(
    val id: Long? = null,
    val sessionId: String,
    val agentId: Long,
    val nodeId: Long? = null,
    val status: AgentInstallSessionNodeStatus = AgentInstallSessionNodeStatus.PENDING,
    val hostname: String? = null,
    val ip: String? = null,
    val errorMessage: String? = null,
    val startedTime: LocalDateTime? = null,
    val finishedTime: LocalDateTime? = null,
    val agentVersion: String? = null,
    val createdTime: LocalDateTime,
    val updatedTime: LocalDateTime
)
