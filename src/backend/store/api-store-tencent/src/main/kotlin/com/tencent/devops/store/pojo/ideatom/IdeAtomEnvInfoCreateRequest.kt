package com.tencent.devops.store.pojo.ideatom

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("IDE插件环境信息添加请求报文体")
data class IdeAtomEnvInfoCreateRequest(
    @ApiModelProperty("插件ID", required = true)
    val atomId: String,
    @ApiModelProperty("插件安装包路径", required = true)
    val pkgPath: String
)