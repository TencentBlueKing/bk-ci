package com.tencent.devops.ticket.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("凭据-更新时内容")
data class CredentialSettingUpdate(
    @ApiModelProperty("凭据是否允许跨项目访问", required = true)
    val allowAcrossProject: Boolean
)
