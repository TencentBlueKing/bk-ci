package com.tencent.devops.plugin.pojo.cos

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("CDN上传文件")
data class CdnUploadFileInfo(
    @ApiModelProperty("文件上传路径（多个路径中间逗号隔开），支持正则表达式", required = true)
    val regexPaths: String,
    @ApiModelProperty("是否自定义归档", required = true)
    val customize: Boolean,
    @ApiModelProperty("凭证ID", required = true)
    val ticketId: String,
    @ApiModelProperty("路径前缀", required = true)
    val cdnPathPrefix: String
)