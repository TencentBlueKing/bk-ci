package com.tencent.devops.remotedev.pojo.expert

data class CreateExpertSupportConfigData(
    val type: ExpertSupportConfigType,
    val content: String
)

enum class ExpertSupportConfigType {
    ERROR,
    SUPPORTER
}

data class FetchExpertSupResp(
    val id: Long,
    val content: String
)
