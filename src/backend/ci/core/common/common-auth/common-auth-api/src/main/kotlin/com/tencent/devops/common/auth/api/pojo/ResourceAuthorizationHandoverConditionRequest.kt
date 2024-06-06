package com.tencent.devops.common.auth.api.pojo

import com.tencent.devops.common.auth.enums.HandoverChannelCode
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源授权交接条件实体")
data class ResourceAuthorizationHandoverConditionRequest(
    @get:Schema(title = "项目ID")
    override val projectCode: String,
    @get:Schema(title = "资源类型")
    override val resourceType: String,
    @get:Schema(title = "资源名称")
    override val resourceName: String? = null,
    @get:Schema(title = "授予人")
    override val handoverFrom: String? = null,
    @get:Schema(title = "greaterThanHandoverTime")
    override val greaterThanHandoverTime: Long? = null,
    @get:Schema(title = "lessThanHandoverTime")
    override val lessThanHandoverTime: Long? = null,
    @Parameter(description = "第几页", required = false)
    override val page: Int? = null,
    @Parameter(description = "每页条数", required = false)
    override val pageSize: Int? = null,
    @get:Schema(title = "是否全量选择")
    val fullSelection: Boolean = false,
    @get:Schema(title = "资源权限交接列表")
    val resourceAuthorizationHandoverList: List<ResourceAuthorizationHandoverDTO> = emptyList(),
    @get:Schema(title = "交接渠道")
    val handoverChannel: HandoverChannelCode,
    @get:Schema(title = "交接人")
    val handoverTo: String? = null,
    @get:Schema(title = "是否为预检查，若为true,不做权限交接；")
    val preCheck: Boolean = false
) : ResourceAuthorizationConditionRequest(
    projectCode = projectCode,
    resourceType = resourceType,
    resourceName = resourceName,
    handoverFrom = handoverFrom,
    greaterThanHandoverTime = greaterThanHandoverTime,
    lessThanHandoverTime = lessThanHandoverTime,
    page = page,
    pageSize = pageSize
)
