package com.tencent.devops.experience.pojo.outer

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("是否可以添加外部用户--返回")
data class OuterCanAddVO(
    @ApiModelProperty("是否成功")
    val successful: Boolean,
    @ApiModelProperty("信息")
    val message: String
)
