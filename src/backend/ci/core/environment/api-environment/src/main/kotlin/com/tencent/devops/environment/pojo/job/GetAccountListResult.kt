package com.tencent.devops.environment.pojo.job

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("查询有权限账号列表")
data class GetAccountListResult(
    @ApiModelProperty(value = "帐号ID")
    val authorizedAccount: List<AuthorizedAccount>?,
    @ApiModelProperty(value = "单次返回最大记录数。最大1000，不传默认为20。", required = true)
    val length: Int
) {
    constructor() : this(null, 0)
}