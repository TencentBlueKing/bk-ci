package com.tencent.devops.quality.api.v2.pojo.op

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@JsonInclude(JsonInclude.Include.ALWAYS)
@ApiModel("质量红线-(模板/指标集)配置展示信息")
data class TemplateData(
    @ApiModelProperty("ID")
    val id: Long,
    @ApiModelProperty("模板名称")
    val name: String?,
    @ApiModelProperty("模板类型(指标集, 模板)")
    val type: String?,
    @ApiModelProperty("描述")
    val desc: String?,
    @ApiModelProperty("研发环节")
    val stage: String?,
    @ApiModelProperty("原子的ClassType")
    val elementType: String?,
    @ApiModelProperty("原子名称")
    val elementName: String?,
    @ApiModelProperty("红线位置(BEFORE, AFTER)")
    val controlPointPostion: String?,
    @ApiModelProperty("是否可用")
    val enable: Boolean?,
    @ApiModelProperty("指标数量")
    val indicatorNum: Int?,
    @ApiModelProperty("指标详情")
    val indicatorDetail: List<TemplateIndicatorMap>?
)