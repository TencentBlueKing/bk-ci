package com.tencent.devops.store.pojo.image.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("安装镜像到项目请求报文")
data class InstallImageReq(
    @ApiModelProperty("项目标识", required = true)
    val projectCodeList: ArrayList<String>,
    @ApiModelProperty("镜像代码", required = true)
    val imageCode: String
)