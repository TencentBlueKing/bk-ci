package com.tencent.devops.environment.pojo.thirdpartyagent

data class OpAgentUpgradeInfo(
    // COMMON
    val maxParallelCount: Int?,
    // WORKER
    val workerVersion: String?,
    val workerForceIds: Set<Long>?,
    val workerLockIds: Set<Long>?,
    val workerPriorityProjectIds: Set<String>?,
    val workerDenyProjectIds: Set<String>?,
    // AGENT
    val agentVersion: String?,
    val agentForceIds: Set<Long>?,
    val agentLockIds: Set<Long>?,
    val agentPriorityProjectIds: Set<String>?,
    val agentDenyProjectIds: Set<String>?,
    val agentMaxUpgradeCount: Long?,
    // JDK
    val jdkVersions: List<JDKInfo>?,
    val jdkForceIds: Set<Long>?,
    val jdkLockIds: Set<Long>?,
    // DOCKER_INIT_FILE
    val dockerInitFileLinuxMd5: String?,
    val dockerInitFileNoLinuxMd5: String?,
    val dockerInitFileForceIds: Set<Long>?,
    val dockerInitFileLockIds: Set<Long>?
)
