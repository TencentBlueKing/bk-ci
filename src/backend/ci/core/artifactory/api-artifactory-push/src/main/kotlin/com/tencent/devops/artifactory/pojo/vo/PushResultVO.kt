package com.tencent.devops.artifactory.pojo.vo

import com.tencent.devops.artifactory.pojo.PushStatus
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("校验审核状态返回信息")
data class PushResultVO(
    @ApiModelProperty("审核状态")
    val status: PushStatus,
    @ApiModelProperty("返回信息")
    val msg: String
)