package com.tencent.devops.common.api.pojo.agent

data class NewHeartbeatInfo(
    val masterVersion: String,
    val slaveVersion: String,
    val hostName: String,
    val agentIp: String,
    val parallelTaskCount: Int,
    val agentInstallPath: String,
    val startedUser: String,
    val taskList: List<ThirdPartyBuildInfo>,
    var agentId: Long?,
    var projectId: String?,
    var heartbeatTime: Long?
) {
    companion object {
        fun dummyHeartbeat(projectId: String, agentId: Long): NewHeartbeatInfo {
            return NewHeartbeatInfo(
                "",
                "",
                "",
                "",
                0,
                "",
                "",
                listOf(),
                agentId,
                projectId,
                System.currentTimeMillis()
            )
        }
    }
}