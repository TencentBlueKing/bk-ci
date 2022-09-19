package com.tencent.devops.repository.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("代码库项目app安装结果")
data class AppInstallationResult(
    @ApiModelProperty("状态")
    val status: Boolean,
    @ApiModelProperty("url地址")
    val url: String = ""
)
