package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("源文件信息")
data class FileSource(
    @ApiModelProperty(value = "文件集合", required = true)
    val fileList: List<String>,
    @ApiModelProperty(value = "源文件服务器", required = true)
    val fileEnv: EnvsetInfo,
    @ApiModelProperty(value = "文件源账号", required = true)
    val fileAccount: String
)