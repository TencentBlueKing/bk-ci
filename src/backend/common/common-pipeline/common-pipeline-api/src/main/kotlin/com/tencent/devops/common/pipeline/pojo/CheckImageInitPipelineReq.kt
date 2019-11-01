package com.tencent.devops.common.pipeline.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("检查镜像合法性初始化流水线请求报文体")
data class CheckImageInitPipelineReq(
    @ApiModelProperty("镜像代码", required = true)
    val imageCode: String,
    @ApiModelProperty("镜像名称", required = true)
    val imageName: String,
    @ApiModelProperty("镜像版本号", required = true)
    val version: String,
    @ApiModelProperty("镜像类型", required = false)
    val imageType: String? = null,
    @ApiModelProperty("仓库用户名", required = false)
    val registryUser: String? = null,
    @ApiModelProperty("仓库密码", required = false)
    val registryPwd: String? = null
)