package com.tencent.devops.store.pojo.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.store.pojo.ExtsionInfoReq
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class InitExtServiceDTO (
    @ApiModelProperty("扩展服务code")
    val serviceCode: String,
    @ApiModelProperty("扩展服务Name")
    val serviceName: String,
    @ApiModelProperty("调试项目Id")
    val itemId: String,
    @ApiModelProperty("扩展点列表")
    val extensionList: List<ExtsionInfoReq>,
    @ApiModelProperty("添加用户")
    val creatorUser: String
)