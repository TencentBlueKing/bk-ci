package com.tencent.devops.turbo.pojo

import com.tencent.devops.common.util.enums.ConfigParamType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("加速模式配置参数")
data class ParamConfigModel(
    @ApiModelProperty("加速模式配置参数key")
    var paramKey: String,
    @ApiModelProperty("加速模式配置参数名")
    var paramName: String,
    @ApiModelProperty("加速模式配置参数类型")
    var paramType: ConfigParamType,
    @ApiModelProperty("加速模式类型属性")
    var paramProps: Map<String, Any?>?,
    @ApiModelProperty("加速模式配置参数枚举")
    var paramEnum: List<ParamEnumModel>?,
    @ApiModelProperty("是否显示")
    var displayed: Boolean = true,
    @ApiModelProperty("默认值")
    var defaultValue: Any? = null,
    @ApiModelProperty("是否必填")
    var required: Boolean? = false,
    @ApiModelProperty("参数tips")
    var tips: String? = null,
    @ApiModelProperty("值为remote表示接口获取,其它则表示默认")
    var dataType: String? = null,
    @ApiModelProperty("与dataType搭配使用,表示接口地址")
    var paramUrl: String? = null
)
