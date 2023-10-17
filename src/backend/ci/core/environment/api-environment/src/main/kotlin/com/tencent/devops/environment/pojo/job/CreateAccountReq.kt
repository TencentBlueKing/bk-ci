package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModelProperty

data class CreateAccountReq(
    @ApiModelProperty(value = "帐号名称", required = true)
    val account: String,
    @ApiModelProperty(value = "账号类型", notes = "1：Linux，2：Windows", required = true)
    val type: Int,
    @ApiModelProperty(value = "账号用途", notes = "1：系统账号", required = true)
    val category: Int,
    @ApiModelProperty(value = "系统账号密码", notes = "账号用途为系统账号 且 账号类型为Windows时,必传")
    val password: String?,
    @ApiModelProperty(value = "别名", notes = "不传则以账号名称作为别名")
    val alias: String?,
    @ApiModelProperty(value = "描述")
    val description: String?
)