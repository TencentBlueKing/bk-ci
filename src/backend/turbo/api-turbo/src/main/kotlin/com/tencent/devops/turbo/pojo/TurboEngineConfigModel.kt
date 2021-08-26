package com.tencent.devops.turbo.pojo

import com.tencent.devops.turbo.validate.TurboEngineConfigGroup
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty

@Suppress("MaxLineLength")
@ApiModel("编译加速模式请求数据模型")
data class TurboEngineConfigModel(
    @ApiModelProperty("编译加速模式代码")
    @get:NotBlank(message = "模板代码不能为空", groups = [TurboEngineConfigGroup.Create::class])
    val engineCode: String?,
    @ApiModelProperty("编译加速模式名字")
    @get:NotBlank(message = "模板名字不能为空", groups = [TurboEngineConfigGroup.Create::class, TurboEngineConfigGroup.Update::class])
    val engineName: String?,
    @ApiModelProperty("描述")
    val desc: String?,
    @ApiModelProperty("spel表达式")
    @get:NotBlank(message = "计算表达式不能为空", groups = [TurboEngineConfigGroup.Create::class, TurboEngineConfigGroup.Update::class])
    val spelExpression: String?,
    @ApiModelProperty("spel参数映射")
    val spelParamMap: Map<String, Any?>?,
    @ApiModelProperty("显式参数配置")
    val paramConfig: List<ParamConfigModel>?,
    @ApiModelProperty("用户手册")
    val userManual: String? = null,
    @ApiModelProperty("文档链接指引")
    val docUrl: String? = null,
    @get:NotEmpty(message = "编译加速历史显示字段值不能为空", groups = [TurboEngineConfigGroup.Create::class, TurboEngineConfigGroup.Update::class])
    @ApiModelProperty("编译加速历史显示字段值")
    val displayFields: List<TurboDisplayFieldModel>? = null,
    @ApiModelProperty("是否推荐")
    val recommend: Boolean? = false,
    @ApiModelProperty("推荐理由")
    val recommendReason: String? = null,
    @ApiModelProperty("插件提示")
    val pluginTips: String? = null
)
