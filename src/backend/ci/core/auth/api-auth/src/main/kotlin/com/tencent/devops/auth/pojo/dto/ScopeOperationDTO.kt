package com.tencent.devops.auth.pojo.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Oauth2授权操作信息请求实体")
data class ScopeOperationDTO(
    @ApiModelProperty("主键ID")
    val id: Int,
    @ApiModelProperty("授权操作ID")
    val operationId: String,
    @ApiModelProperty("授权操作中文名称")
    val operationNameCn: String,
    @ApiModelProperty("授权操作英文名称")
    val operationNameEn: String
)
