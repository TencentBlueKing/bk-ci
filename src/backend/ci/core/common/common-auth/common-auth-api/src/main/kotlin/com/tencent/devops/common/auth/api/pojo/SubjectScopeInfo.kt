package com.tencent.devops.common.auth.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.auth.enums.SubjectScopeType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "授权范围")
data class SubjectScopeInfo(
    @Schema(name = "ID")
    val id: String?,
    @Schema(name = "name")
    val name: String,
    @Schema(name = "类型")
    val type: String? = SubjectScopeType.USER.value,
    @JsonProperty("full_name")
    val fullName: String? = "",
    @Schema(name = "用户名")
    val username: String? = ""
)
