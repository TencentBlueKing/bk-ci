package com.tencent.devops.experience.pojo.outer

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("是否可以添加外部用户--返回")
data class OuterCanAddVO(
    @ApiModelProperty("合法人员列表")
    val legalUserIds: List<String>,
    @ApiModelProperty("不合法人员列表")
    val illegalUserIds: List<String>
)
