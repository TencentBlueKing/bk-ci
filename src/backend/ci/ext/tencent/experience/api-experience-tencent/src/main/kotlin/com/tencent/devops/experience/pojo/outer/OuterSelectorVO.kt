package com.tencent.devops.experience.pojo.outer

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("外部用户--选择信息")
data class OuterSelectorVO(
    @ApiModelProperty("ID")
    val username: String
)
