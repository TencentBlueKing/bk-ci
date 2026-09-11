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

data class AgentInstallSession(
    val id: String,
    val projectId: String,
    val mode: AgentInstallSessionMode,
    val createdBy: String,
    val os: String,
    val zone: String?,
    val gateway: String,
    val fileGateway: String?,
    val installType: String,
    val agentType: String,
    val loginName: String? = null,
    val loginPasswordCipher: String? = null,
    val parallelTaskCount: Int,
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
