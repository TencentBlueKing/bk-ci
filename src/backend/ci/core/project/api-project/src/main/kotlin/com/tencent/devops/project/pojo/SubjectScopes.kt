package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目-最大可授权人员范围")
data class SubjectScope(
    @ApiModelProperty("id")
    val id: String,
    @ApiModelProperty("类型")
    val type: String
)
