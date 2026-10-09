package com.tencent.devops.environment.pojo.thirdpartyagent

data class BatchFetchNodeInfoData(
    val agentHashIdList: Set<String>
)

data class BatchFetchNodeInfoResp(
    val agentHashId: String,
    val nodeHashId: String,
    val nodeName: String?
)
