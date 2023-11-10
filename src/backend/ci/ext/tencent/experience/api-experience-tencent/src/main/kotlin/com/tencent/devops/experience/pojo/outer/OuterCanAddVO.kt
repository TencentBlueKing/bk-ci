package com.tencent.devops.experience.pojo.outer

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("是否可以添加外部用户--返回")
data class OuterCanAddVO(
    @ApiModelProperty("成功人员列表")
    val successUserIds: List<String>,
    @ApiModelProperty("失败人员列表")
    val failedUserIds: List<String>,
    @ApiModelProperty("已经存在的人员列表")
    val existUserIds: List<String>
)
