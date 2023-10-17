package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModelProperty

data class DeleteAccountReq(
    @ApiModelProperty(value = "帐号ID", required = true)
    val id: String
)