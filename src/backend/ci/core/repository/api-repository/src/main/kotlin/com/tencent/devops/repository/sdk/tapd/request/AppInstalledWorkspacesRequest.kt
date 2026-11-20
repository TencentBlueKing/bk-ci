package com.tencent.devops.repository.sdk.tapd.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.repository.sdk.common.enums.HttpMethod
import com.tencent.devops.repository.sdk.tapd.TapdRequest
import com.tencent.devops.scm.pojo.tapd.InstalledWorkspacesResponse
import com.tencent.devops.scm.pojo.tapd.TapdResult

/**
 * 获取安装了当前应用的 TAPD 项目
 *
 * 接口：`GET /workspaces/app_installed_workspaces?page=1&per_page=200`
 */
data class AppInstalledWorkspacesRequest(
    @JsonProperty("page")
    val page: Int = 1,
    @JsonProperty("per_page")
    val perPage: Int = 200,
    @JsonProperty("workspace_id")
    val workspaceId: Int? = null,
    @JsonProperty("app_version")
    val appVersion: String? = null
) : TapdRequest<TapdResult<InstalledWorkspacesResponse>>() {

    override fun getHttpMethod(): HttpMethod = HttpMethod.GET

    override fun getApiPath(): String = "workspaces/app_installed_workspaces"
}
