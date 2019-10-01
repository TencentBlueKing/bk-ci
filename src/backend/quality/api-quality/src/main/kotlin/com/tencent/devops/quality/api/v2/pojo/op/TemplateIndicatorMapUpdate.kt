package com.tencent.devops.quality.api.v2.pojo.op

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@JsonInclude(JsonInclude.Include.ALWAYS)
@ApiModel("质量红线-(模板/指标集)与指标关联信息创建/更新 模型")
data class TemplateIndicatorMapUpdate(
    @ApiModelProperty("模板ID")
    var templateId: Long?,
    @ApiModelProperty("指标ID")
    val indicatorId: Long?,
    @ApiModelProperty("可选操作")
    val operation: String?,
    @ApiModelProperty("阈值")
    val threshold: String?
)