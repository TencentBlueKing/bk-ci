package com.tencent.devops.plugin.pojo.cos

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Spm上传文件对应的下载地址")
data class SpmFile(
    @ApiModelProperty("uploadTaskKey", required = true)
    val uploadTaskKey: String,
    @ApiModelProperty("cdnPath", required = true)
    val cdnPath: String
)