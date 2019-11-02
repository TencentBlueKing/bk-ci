package com.tencent.devops.dockerhost.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("验证镜像合法性请求报文体")
data class CheckImageRequest(
    @ApiModelProperty("镜像类型", required = false)
    val imageType: String?,
    @ApiModelProperty("镜像名称", required = true)
    val imageName: String,
    @ApiModelProperty("用户名", required = false)
    val registryUser: String?,
    @ApiModelProperty("密码", required = false)
    val registryPwd: String?
)