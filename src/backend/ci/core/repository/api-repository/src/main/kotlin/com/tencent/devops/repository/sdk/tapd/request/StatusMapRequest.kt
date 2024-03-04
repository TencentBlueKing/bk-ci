package com.tencent.devops.repository.sdk.tapd.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.repository.sdk.common.enums.HttpMethod
import com.tencent.devops.repository.sdk.tapd.TapdRequest
import com.tencent.devops.repository.sdk.tapd.TapdResult

class StatusMapRequest(
    // 项目ID
    @JsonProperty("workspace_id")
    val workspaceId: Int,
    // 系统名。取 bug （缺陷的）或者 story（需求的）
    val system: String
) : TapdRequest<TapdResult<Map<String, String>>>() {

    override fun getHttpMethod(): HttpMethod {
        return HttpMethod.GET
    }

    override fun getApiPath(): String {
        return "workflows/status_map"
    }
}
