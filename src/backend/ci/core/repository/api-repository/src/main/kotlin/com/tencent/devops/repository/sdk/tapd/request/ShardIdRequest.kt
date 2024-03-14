package com.tencent.devops.repository.sdk.tapd.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.repository.sdk.common.enums.HttpMethod
import com.tencent.devops.repository.sdk.tapd.TapdRequest
import com.tencent.devops.repository.sdk.tapd.TapdResult
import com.tencent.devops.repository.sdk.tapd.response.ShardIdResponse

data class ShardIdRequest(
    // 项目ID
    @JsonProperty("workspace_id")
    val workspaceId: Int? = null,
    // 业务对象类型。取值 story 、 bug 、 task
    val type: String,
    // 短ID,支持多ID查询
    @JsonProperty("short_id")
    val shortId: String? = null,
    // 长ID,支持多ID查询
    @JsonProperty("long_id")
    val longId: String? = null,
    // 设置返回数量限制，默认为30
    val limit: Int = 30,
    // 返回当前数量限制下第N页的数据，默认为1（第一页）
    val page: Int = 1,
    // 排序规则，规则：字段名 ASC或者DESC
    val order: String? = null,
    // 设置获取的字段，多个字段间以','逗号隔开
    val fields: String? = null
) : TapdRequest<TapdResult<List<ShardIdResponse>>>() {
    override fun getHttpMethod(): HttpMethod {
        return HttpMethod.GET
    }

    override fun getApiPath(): String {
        return "shard_id"
    }
}
