
package com.tencent.devops.turbo.vo

import com.tencent.devops.turbo.pojo.ParamConfigModel
import com.tencent.devops.turbo.pojo.TurboDisplayFieldModel
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("编译加速方案-初始页视图")
data class TurboEngineConfigVO(
    @ApiModelProperty("蓝盾模板代码")
    var engineCode: String = "",

    @ApiModelProperty("编译加速模式名称")
    var engineName: String? = null,

    @ApiModelProperty("优先级")
    var priorityNum: Int? = null,

    @ApiModelProperty("编译加速模型描述")
    var desc: String = "",

    @ApiModelProperty("编译加速节约时间计算表达式")
    var spelExpression: String? = null,

    @ApiModelProperty("编译加速参数数组")
    var spelParamMap: Map<String, Any?>? = null,

    @ApiModelProperty("是否启用")
    var enabled: Boolean? = null,

    @ApiModelProperty("使用指引")
    var userManual: String? = null,

    @ApiModelProperty("doc_url")
    var docUrl: String? = null,

    @ApiModelProperty("参数配置清单")
    var paramConfig: List<ParamConfigModel>?,

    @ApiModelProperty("显示字段清单")
    var displayFields: List<TurboDisplayFieldModel>? = null,

    @ApiModelProperty("是否推荐")
    var recommend: Boolean?,

    @ApiModelProperty("推荐原因")
    var recommendReason: String?,

    @ApiModelProperty("插件提示配置")
    var pluginTips: String?,

    @ApiModelProperty("最近修改人")
    var updatedBy: String? = null,

    @ApiModelProperty("最近修改时间")
    var updatedDate: LocalDateTime? = null

)
