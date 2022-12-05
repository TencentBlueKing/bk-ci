package com.tencent.bkrepo.scanner.pojo.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.bkrepo.scanner.pojo.ScanTriggerType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("匹配所有扫描计划扫描单个文件请求")
@JsonIgnoreProperties(ignoreUnknown = true)
data class MatchPlanSingleScanRequest(
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("仓库名")
    val repoName: String,
    @ApiModelProperty("fullPath")
    val fullPath: String? = null,

    @ApiModelProperty("packageName")
    val packageName: String? = null,
    @ApiModelProperty("packageKey")
    val packageKey: String? = null,
    @ApiModelProperty("version")
    val version: String? = null,
    @ApiModelProperty("扫描触发方式")
    val triggerType: String = ScanTriggerType.ON_NEW_ARTIFACT.name
)
