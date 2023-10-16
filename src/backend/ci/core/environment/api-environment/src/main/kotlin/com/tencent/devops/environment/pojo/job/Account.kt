package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("源文件帐号信息")
data class Account(
    @ApiModelProperty(value = "源文件执行帐号用户名")
    val alias: String?,
    @ApiModelProperty(value = "源文件执行帐号ID")
    val id: Long?
)