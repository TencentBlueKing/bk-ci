package com.tencent.devops.environment.pojo.job.cmdbres

import io.swagger.annotations.ApiModelProperty

data class CmdbHeader(
    @ApiModelProperty(value = "实际返回的条目数", required = true)
    val returnRows: Int,
    @ApiModelProperty(value = "总的条目数", required = true)
    val totalRows: Int
)