package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("最新动态-修改模型")
data class OPActivityUpdate(
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("类型")
    val type: String,
    @ApiModelProperty("状态")
    val status: String,
    @ApiModelProperty("链接")
    val link: String
)