package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户部门详细信息")
data class BkUserDeptInfo(
    @get:Schema(title = "id")
    val id: String?,
    @get:Schema(title = "部门名称")
    val name: String?,
    @get:Schema(title = "部门详细名称")
    @JsonProperty("full_name")
    val fullName: String?
)
