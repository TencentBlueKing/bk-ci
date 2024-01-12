package com.tencent.devops.auth.pojo.vo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "项目权限信息")
data class ProjectPermissionInfoVO(
    @Schema(name = "项目ID")
    @JsonProperty("project_id")
    val projectCode: String,
    @Schema(name = "项目名称")
    @JsonProperty("project_name")
    val projectName: String,
    @Schema(name = "创建人")
    val creator: String,
    @Schema(name = "管理员")
    val owners: List<String>,
    @Schema(name = "项目成员")
    val members: List<String>
)
