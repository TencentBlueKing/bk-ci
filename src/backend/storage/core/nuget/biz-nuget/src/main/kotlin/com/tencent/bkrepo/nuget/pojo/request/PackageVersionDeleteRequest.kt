package com.tencent.bkrepo.nuget.pojo.request

import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation

@ApiOperation("删除包版本请求")
data class PackageVersionDeleteRequest(
    @ApiModelProperty("所属项目", required = true)
    val projectId: String,
    @ApiModelProperty("仓库名称", required = true)
    val repoName: String,
    @ApiModelProperty("包名称", required = true)
    val name: String,
    @ApiModelProperty("包的版本", required = true)
    val version: String,
    @ApiModelProperty("操作用户", required = true)
    val operator: String
)
