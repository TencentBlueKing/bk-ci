package com.tencent.devops.quality.api.v2.pojo.op

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@JsonInclude(JsonInclude.Include.ALWAYS)
@ApiModel("质量红线-指标配置列表信息")
data class IndicatorData(
    @ApiModelProperty("ID")
    val id: Long,
    @ApiModelProperty("原子的ClassType")
    val elementType: String?,
    @ApiModelProperty("原子名称")
    val elementName: String?,
    @ApiModelProperty("工具/原子子类")
    val elementDetail: String?,
    @ApiModelProperty("指标英文名")
    val enName: String?,
    @ApiModelProperty("指标中文名")
    val cnName: String?,
    @ApiModelProperty("指标所包含基础数据")
    val metadataIds: String?,
    @ApiModelProperty("指标所包含基础数据")
    val metadataNames: String?,
    @ApiModelProperty("默认操作类型")
    val defaultOperation: String?,
    @ApiModelProperty("可用操作")
    val operationAvailable: String?,
    @ApiModelProperty("默认阈值")
    val threshold: String?,
    @ApiModelProperty("阈值类型")
    val thresholdType: String?,
    @ApiModelProperty("描述")
    val desc: String?,
    @ApiModelProperty("是否可修改")
    val readOnly: Boolean?,
    @ApiModelProperty("阶段")
    val stage: String?,
    @ApiModelProperty("可见范围类型(ANY, PART_BY_NAME)")
    val range: String?,
    @ApiModelProperty("指标类型")
    val type: String?,
    @ApiModelProperty("指标标签，用于前端区分控制")
    val tag: String?,
    @ApiModelProperty("是否启用")
    val enable: Boolean?
)