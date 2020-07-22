package com.tencent.devops.common.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("状态")
data class SimpleResult(
        @ApiModelProperty("是否成功", required = true)
        val success: Boolean,
        @ApiModelProperty("错误信息", required = false)
        val message: String? = null
)