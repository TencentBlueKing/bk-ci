package com.tencent.devops.gitci.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("环境变量")
data class EnvironmentVariables(
    @ApiModelProperty("name")
    val name: String,
    @ApiModelProperty("value")
    val value: String,
    @ApiModelProperty("branch")
    val branch: String?,
    @ApiModelProperty("display value in build log")
    val display: Boolean
)
