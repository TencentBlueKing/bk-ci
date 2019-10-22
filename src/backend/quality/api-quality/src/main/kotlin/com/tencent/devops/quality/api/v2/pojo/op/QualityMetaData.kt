package com.tencent.devops.quality.api.v2.pojo.op

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@JsonInclude(JsonInclude.Include.ALWAYS)
@ApiModel("质量红线-基础数据列表展示信息")
data class QualityMetaData(
    @ApiModelProperty("ID")
    val id: Long,
    @ApiModelProperty("数据ID")
    val dataId: String?,
    @ApiModelProperty("基础数据名称")
    val dataName: String?,
    @ApiModelProperty("原子的ClassType")
    val elementType: String?,
    @ApiModelProperty("产出原子")
    val elementName: String?,
    @ApiModelProperty("工具/原子子类")
    val elementDetail: String?,
    @ApiModelProperty("数值类型")
    val valueType: String?,
    @ApiModelProperty("说明")
    val desc: String?,
    @ApiModelProperty("备注")
    var extra: String?
)