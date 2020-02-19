package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModelProperty

data class ExtServiceEnvCreateInfo(
    @ApiModelProperty("扩展服务Id")
    val serviceId: String,
    @ApiModelProperty("扩展服务开发语言")
    val language: String? = "",
    @ApiModelProperty("扩展服务执行包路径")
    val pkgPath: String? = "",
    @ApiModelProperty("扩展服务执行包SHA签名串")
    val pkgShaContent: String? = "",
    @ApiModelProperty("dockefile内容")
    val dockerFileContent: String? = "",
    @ApiModelProperty("扩展服务镜像路径")
    val imagePath: String? = null,
    @ApiModelProperty("扩展服务前端入口文件")
    val frontentEntryFile: String?,
    @ApiModelProperty("添加用户")
    val creatorUser: String,
    @ApiModelProperty("修改用户")
    val modifierUser: String
)