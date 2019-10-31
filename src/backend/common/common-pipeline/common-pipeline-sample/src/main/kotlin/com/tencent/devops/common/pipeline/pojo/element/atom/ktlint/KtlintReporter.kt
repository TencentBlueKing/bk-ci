package com.tencent.devops.common.pipeline.pojo.element.atom.ktlint

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("ktlint reporter")
data class KtlintReporter(
    @ApiModelProperty("reporter", required = true)
    val reporter: KtlintReporterType,
    @ApiModelProperty("报告保存路径", required = false)
    val reportOutput: String?,
    @ApiModelProperty("报告在构件详情中显示的名字", required = false)
    val reportName: String?
)
