package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("根据account查询其别名的结果")
data class QueryAccountAliasResult (
    @ApiModelProperty(value = "account别名", required = true)
    val accountAlias: String
)