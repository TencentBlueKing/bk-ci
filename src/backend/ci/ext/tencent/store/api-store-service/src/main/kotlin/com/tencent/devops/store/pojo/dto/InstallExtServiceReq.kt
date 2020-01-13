package com.tencent.devops.store.pojo.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("安装扩展服务到项目")
data class InstallExtServiceReq (
    @ApiModelProperty("项目标识", required = true)
    val projectCodeList: ArrayList<String>,
    @ApiModelProperty("扩展服务编码", required = true)
    val serviceCode: String
)