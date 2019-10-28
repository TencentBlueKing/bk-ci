package com.tencent.devops.support.model.approval

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("创建审批单请求报文体")
data class CreateMoaApproveRequest(
    @ApiModelProperty("审批人，多个以逗号分隔", required = true)
    val verifier: String,
    @ApiModelProperty("标题", required = true)
    val title: String,
    @ApiModelProperty("任务ID", required = true)
    val taskId: String,
    @ApiModelProperty("回调URL", required = true)
    val backUrl: String,
    @ApiModelProperty("系统URL，用于用户审核时跳转系统查看", required = false)
    val sysUrl: String? = null
)