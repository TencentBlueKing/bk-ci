package com.tencent.devops.process.pojo

import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("检查镜像合法性初始化流水线报文响应体")
data class CheckImageInitPipelineResp(
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("构建ID", required = false)
    val buildId: String?,
    @ApiModelProperty("验证状态", required = true)
    val imageCheckStatus: ImageStatusEnum
)