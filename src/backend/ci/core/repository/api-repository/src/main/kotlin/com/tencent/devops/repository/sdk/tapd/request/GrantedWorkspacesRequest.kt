package com.tencent.devops.repository.sdk.tapd.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.repository.sdk.common.enums.HttpMethod
import com.tencent.devops.repository.sdk.tapd.TapdRequest
import com.tencent.devops.scm.pojo.tapd.GrantedWorkspacesResponse
import com.tencent.devops.scm.pojo.tapd.TapdResult

/**
 * 获取应用已授权的 TAPD 项目
 *
 * 接口：`GET /app_auth/get_granted_workspaces?limit=200&page=1`
 */
data class GrantedWorkspacesRequest(
    @JsonProperty("limit")
    val limit: Int = 200,
    @JsonProperty("page")
    val page: Int = 1,
    @JsonProperty("workspace_id")
    val workspaceId: Int? = null
) : TapdRequest<TapdResult<GrantedWorkspacesResponse>>() {

    override fun getHttpMethod(): HttpMethod = HttpMethod.GET

    override fun getApiPath(): String = "app_auth/get_granted_workspaces"
}
