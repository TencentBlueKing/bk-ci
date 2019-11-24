package com.tencent.devops.gitci.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("分页-基本信息")
data class GitProjectConfWithPage(
    @ApiModelProperty("总数", required = true)
    val total: Int,
    @ApiModelProperty("列表", required = true)
    val data: List<GitProjectConf>
)