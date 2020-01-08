package com.tencent.devops.store.pojo.vo

import io.swagger.annotations.ApiModelProperty

data class ExtServiceStatVO (
    @ApiModelProperty("扩展服务code")
    val serviceCode: String,
    @ApiModelProperty("扩展服务Id")
    val serviceId: String,
    @ApiModelProperty("安装量")
    val installationCount: Int,
    @ApiModelProperty("评论数")
    val conmentCount: Int,
    @ApiModelProperty("星级")
    val star: String
)