package com.tencent.devops.common.auth.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.auth.enums.SubjectScopeType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("授权范围")
data class SubjectScopeInfo(
    @ApiModelProperty("ID")
    val id: String?,
    @ApiModelProperty("name")
    val name: String,
    @ApiModelProperty("类型")
    val type: String? = SubjectScopeType.USER.value,
    @JsonProperty("full_name")
    val fullName: String? = "",
    @ApiModelProperty("用户名")
    val username: String? = ""
)
