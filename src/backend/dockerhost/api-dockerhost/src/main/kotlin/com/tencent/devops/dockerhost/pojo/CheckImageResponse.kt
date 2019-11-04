package com.tencent.devops.dockerhost.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("验证镜像合法性返回报文体")
data class CheckImageResponse(
    @ApiModelProperty("作者", required = false)
    val author: String?,
    @ApiModelProperty("评论", required = false)
    val comment: String?,
    @ApiModelProperty("镜像大小", required = true)
    val size: Long,
    @ApiModelProperty("虚拟大小", required = false)
    val virtualSize: Long?,
    @ApiModelProperty("tag列表", required = true)
    val repoTags: List<String>
)