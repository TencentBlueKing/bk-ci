package com.tencent.devops.plugin.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("构建模型-parameters")
data class ParametersInfo(
    @ApiModelProperty("参数key值", required = true)
    val key: String,
    @ApiModelProperty("key值是否可以更改", required = true)
    val keyDisable: Boolean,
    @ApiModelProperty("key值前端组件类型", required = true)
    val keyType: String,
    @ApiModelProperty("key值获取方式", required = true)
    val keyListType: String,
    @ApiModelProperty("key值获取路径", required = true)
    val keyUrl: String,
    @ApiModelProperty
    val keyUrlQuery: List<String>,
    @ApiModelProperty("key值获取集合", required = true)
    val keyList: List<ParameterListInfo>,
    @ApiModelProperty("key值是否多选", required = true)
    val keyMultiple: Boolean,
    @ApiModelProperty("参数value值", required = true)
    val value: String,
    @ApiModelProperty("value值是否可以更改", required = true)
    val valueDisable: Boolean,
    @ApiModelProperty("value值前端组件类型", required = true)
    val valueType: String,
    @ApiModelProperty("value值获取方式", required = true)
    val valueListType: String,
    @ApiModelProperty("value值获取路径", required = true)
    val valueUrl: String,
    @ApiModelProperty
    val valueUrlQuery: List<String>,
    @ApiModelProperty("value值获取集合", required = true)
    val valueList: List<ParameterListInfo>,
    @ApiModelProperty("value值是否多选", required = true)
    val valueMultiple: Boolean
)
