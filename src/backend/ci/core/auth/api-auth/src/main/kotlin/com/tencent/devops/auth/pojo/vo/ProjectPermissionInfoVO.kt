package com.tencent.devops.auth.pojo.vo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目权限信息")
data class ProjectPermissionInfoVO(
    @get:Schema(title = "项目ID")
    @JsonProperty("project_id")
    val projectCode: String,
    @get:Schema(title = "项目名称")
    @JsonProperty("project_name")
    val projectName: String,
    @get:Schema(title = "创建人")
    val creator: String,
    @get:Schema(title = "管理员")
    val owners: List<String>,
    @get:Schema(title = "项目成员")
    val members: List<String>
)
