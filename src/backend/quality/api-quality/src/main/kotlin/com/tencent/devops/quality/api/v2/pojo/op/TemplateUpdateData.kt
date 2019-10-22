package com.tencent.devops.quality.api.v2.pojo.op

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@JsonInclude(JsonInclude.Include.ALWAYS)
@ApiModel("质量红线-(模板/指标集)配置更新模型")
data class TemplateUpdateData(
    @ApiModelProperty("指标详情", required = true)
    val templateUpdate: TemplateUpdate,
    @ApiModelProperty("指标详情")
    val indicatorDetail: List<TemplateIndicatorMapUpdate>?
)