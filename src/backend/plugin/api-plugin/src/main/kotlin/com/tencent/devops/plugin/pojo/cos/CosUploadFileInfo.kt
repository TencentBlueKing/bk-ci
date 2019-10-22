package com.tencent.devops.plugin.pojo.cos

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("COS上传文件")
data class CosUploadFileInfo(
    @ApiModelProperty("bucket", required = true)
    val bucket: String,
    @ApiModelProperty("凭证ID", required = true)
    val ticketId: String,
    @ApiModelProperty("文件上传路径（多个路径中间逗号隔开），支持正则表达式", required = true)
    val regexPaths: String,
    @ApiModelProperty("是否自定义归档", required = true)
    val customize: Boolean,
    @ApiModelProperty("下载URL过期时间", required = false)
    val expireSeconds: Long
)