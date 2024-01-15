package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "用户部门详细信息")
data class BkUserDeptInfo(
    @Schema(name = "id")
    val id: String?,
    @Schema(name = "部门名称")
    val name: String?,
    @Schema(name = "部门详细名称")
    @JsonProperty("full_name")
    val fullName: String?
)
