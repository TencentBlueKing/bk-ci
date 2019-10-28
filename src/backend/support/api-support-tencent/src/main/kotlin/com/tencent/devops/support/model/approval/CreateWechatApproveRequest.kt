package com.tencent.devops.support.model.approval

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("创建微信审批单请求报文体")
data class CreateWechatApproveRequest(
    @ApiModelProperty("蓝鲸APP名称", required = true)
    val appName: String,
    @ApiModelProperty("审批人，多个以逗号分隔", required = true)
    val verifier: String,
    @ApiModelProperty("消息内容", required = true)
    val message: String,
    @ApiModelProperty("任务ID", required = true)
    val taskId: String,
    @ApiModelProperty("回调URL", required = false)
    val url: String? = null
)