package com.tencent.devops.repository.sdk.tapd.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.repository.sdk.common.enums.HttpMethod
import com.tencent.devops.repository.sdk.tapd.TapdRequest
import com.tencent.devops.scm.pojo.tapd.TapdResult
import com.tencent.devops.scm.pojo.tapd.TapdStoryFieldConfig

/**
 * 获取 TAPD 需求所有字段及候选值
 *
 * 接口：`GET /stories/get_fields_info?workspace_id=xx`
 */
data class GetStoryFieldRequest(
    @JsonProperty("workspace_id")
    val workspaceId: String
) : TapdRequest<TapdResult<TapdStoryFieldConfig>>() {

    override fun getHttpMethod(): HttpMethod = HttpMethod.GET

    override fun getApiPath(): String = "stories/get_fields_info"
}
