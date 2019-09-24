package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("复制流水线构建归档到自定义仓库请求")
data class CopyToCustomReq(
    @ApiModelProperty("文件列表", required = true)
    val files: List<String>,
    @ApiModelProperty("是否拷贝左右文件", required = true)
    val copyAll: Boolean
)