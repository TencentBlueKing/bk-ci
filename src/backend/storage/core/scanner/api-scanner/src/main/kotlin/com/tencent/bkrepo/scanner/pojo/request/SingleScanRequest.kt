package com.tencent.bkrepo.scanner.pojo.request

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("单个制品扫描请求")
@JsonIgnoreProperties(ignoreUnknown = true)
data class SingleScanRequest(
    @ApiModelProperty("方案ID")
    @JsonAlias("id")
    val planId: String,
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("仓库名")
    val repoName: String,
    @ApiModelProperty("fullPath")
    var fullPath: String?,
    @ApiModelProperty("packageKey")
    val packageKey: String?,
    @ApiModelProperty("version")
    val version: String?
)
