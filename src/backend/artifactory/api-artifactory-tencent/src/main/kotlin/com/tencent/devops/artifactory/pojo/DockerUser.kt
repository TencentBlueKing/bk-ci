package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("镜像仓库-用户密码")
data class DockerUser(
    @ApiModelProperty("用户名", required = true)
    val user: String,
    @ApiModelProperty("密码", required = true)
    val password: String
)