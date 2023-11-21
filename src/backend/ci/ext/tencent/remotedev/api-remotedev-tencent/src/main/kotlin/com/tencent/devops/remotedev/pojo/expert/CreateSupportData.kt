package com.tencent.devops.remotedev.pojo.expert

data class CreateSupportData(
    val projectId: String,
    val hostIp: String,
    val creator: String,
    val content: String,
    val workspaceName: String,
    val city: String,
    val machineType: String
)

enum class ExpertSupportStatus {
    CREATE,
    RUNNING,
    DONE
}

data class UpdateSupportData(
    val id: Long,
    val status: ExpertSupportStatus,
    val supporter: String?
)

data class FetchSupportResp(
    val id: Long,
    val creator: String,
    val content: String,
    val createTime: String
)
