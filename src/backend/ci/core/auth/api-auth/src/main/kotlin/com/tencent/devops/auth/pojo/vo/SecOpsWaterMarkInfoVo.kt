package com.tencent.devops.auth.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("用户水印信息")
data class SecOpsWaterMarkInfoVo(
    @ApiModelProperty("类型")
    val type: String,
    @ApiModelProperty("水印信息")
    val data: String
)
