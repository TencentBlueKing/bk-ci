package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("用户额外信息")
data class BkUserExtras(
    @ApiModelProperty("性别")
    val gender: String,
    @ApiModelProperty("postName")
    @JsonProperty("postname")
    val postName: String
)
