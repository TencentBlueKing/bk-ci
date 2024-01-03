package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModelProperty

data class Account(
    @ApiModelProperty(value = "账号ID")
    val id: Long,
    @ApiModelProperty(value = "账号名称")
    val name: String?,
    @ApiModelProperty(value = "账号别名")
    val alias: String?
)