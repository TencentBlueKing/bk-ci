package com.tencent.devops.repository.sdk.tapd.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.repository.sdk.common.enums.HttpMethod
import com.tencent.devops.repository.sdk.tapd.TapdRequest
import com.tencent.devops.scm.pojo.tapd.ParticipantWorkspaceResponse
import com.tencent.devops.scm.pojo.tapd.TapdResult

/**
 * 获取指定用户参与的 TAPD 项目列表
 *
 * 接口：`GET /workspaces/user_participant_projects?nick=xx`
 */
data class UserParticipantProjectsRequest(
    @JsonProperty("nick")
    val nick: String,
    @JsonProperty("status")
    val status: String? = null
) : TapdRequest<TapdResult<List<ParticipantWorkspaceResponse>>>() {

    override fun getHttpMethod(): HttpMethod = HttpMethod.GET

    override fun getApiPath(): String = "workspaces/user_participant_projects"
}
