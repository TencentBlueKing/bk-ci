package com.tencent.devops.common.auth.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.auth.enums.SubjectScopeType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "授权范围")
data class SubjectScopeInfo(
    @get:Schema(title = "ID")
    val id: String?,
    @get:Schema(title = "name")
    val name: String,
    @get:Schema(title = "类型")
    val type: String? = SubjectScopeType.USER.value,
    @JsonProperty("full_name")
    val fullName: String? = "",
    @get:Schema(title = "用户名")
    val username: String? = ""
)
