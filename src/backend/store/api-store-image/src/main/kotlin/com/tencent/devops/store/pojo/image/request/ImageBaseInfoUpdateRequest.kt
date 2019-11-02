package com.tencent.devops.store.pojo.image.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("镜像基本信息修改请求报文体")
data class ImageBaseInfoUpdateRequest(
    @ApiModelProperty("插件名称", required = false)
    val imageName: String? = null,
    @ApiModelProperty("插件简介", required = false)
    val summary: String? = null,
    @ApiModelProperty("插件描述", required = false)
    val description: String? = null,
    @ApiModelProperty("插件logo", required = false)
    val logoUrl: String? = null,
    @ApiModelProperty("发布者", required = false)
    val publisher: String? = null,
    @ApiModelProperty(value = "镜像大小", required = false)
    val imageSize: String? = null
)