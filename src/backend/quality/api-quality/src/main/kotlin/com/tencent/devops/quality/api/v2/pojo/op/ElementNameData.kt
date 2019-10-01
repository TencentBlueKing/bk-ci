package com.tencent.devops.quality.api.v2.pojo.op

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@JsonInclude(JsonInclude.Include.ALWAYS)
@ApiModel("质量红线-原子名称下拉列表键值对")
data class ElementNameData(
    @ApiModelProperty("原子的ClassType")
    val elementType: String?,
    @ApiModelProperty("原子中文名称")
    val elementName: String?
)