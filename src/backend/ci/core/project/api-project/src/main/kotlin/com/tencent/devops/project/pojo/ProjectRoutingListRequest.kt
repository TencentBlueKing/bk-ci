package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "路由名单管理请求（白名单/黑名单增删）")
data class ProjectRoutingListRequest(
    @get:JsonProperty(value = "projectCodes", required = true)
    @get:Schema(title = "项目英文名列表（projectId = english_name）", description = "projectCodes")
    val projectCodes: List<String>
)
