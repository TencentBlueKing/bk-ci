package com.tencent.devops.repository.sdk.tapd.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.repository.sdk.common.enums.HttpMethod
import com.tencent.devops.repository.sdk.tapd.TapdRequest
import com.tencent.devops.scm.pojo.tapd.TapdResult
import com.tencent.devops.scm.pojo.tapd.WorkspaceResponse

/**
 * 查询 TAPD 项目信息请求
 *
 * 接口：`GET /workspaces/get_workspace_info?workspace_id=xx`
 */
data class GetWorkspaceInfoRequest(
    /** TAPD 项目 ID */
    @JsonProperty("workspace_id")
    val workspaceId: String
) : TapdRequest<TapdResult<WorkspaceResponse>>() {

    override fun getHttpMethod(): HttpMethod = HttpMethod.GET

    override fun getApiPath(): String = "workspaces/get_workspace_info"
}
