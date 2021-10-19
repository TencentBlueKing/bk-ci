package com.tencent.devops.turbo.pojo

import com.tencent.devops.turbo.validate.TurboPlanGroup
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@ApiModel("加速方案请求数据信息")
data class TurboPlanModel(
    @ApiModelProperty("蓝盾项目id")
    @get:NotBlank(
        message = "项目id不能为空",
        groups = [
            TurboPlanGroup.Create::class,
            TurboPlanGroup.UpdateDetail::class
        ]
    )
    val projectId: String?,
    @ApiModelProperty("加速方案名称")
    @get:NotBlank(
        message = "方案名称不能为空",
        groups = [
            TurboPlanGroup.Create::class,
            TurboPlanGroup.UpdateDetail::class,
            TurboPlanGroup.UpdateAll::class
        ]
    )
    val planName: String?,
    @ApiModelProperty("蓝盾模板代码")
    @get:NotBlank(
        message = "请先选择加速模式！",
        groups = [
            TurboPlanGroup.Create::class,
            TurboPlanGroup.UpdateWhiteList::class,
            TurboPlanGroup.UpdateAll::class
        ]
    )
    val engineCode: String?,
    @ApiModelProperty("方案说明")
    val desc: String?,
    @ApiModelProperty("配置参数值")
    @get:NotNull(
        message = "参数不能为空",
        groups = [
            TurboPlanGroup.UpdateParam::class,
            TurboPlanGroup.UpdateAll::class
        ]
    )
    @get:NotEmpty(
        message = "参数不能为空",
        groups = [
            TurboPlanGroup.UpdateParam::class,
            TurboPlanGroup.UpdateAll::class
        ]
    )
    val configParam: Map<String, Any>?,
    @ApiModelProperty("白名单")
    @get:NotBlank(
        message = "白名单不能为空",
        groups = [
            TurboPlanGroup.UpdateWhiteList::class
        ]
    )
    val whiteList: String?,
    @ApiModelProperty("开启状态")
    @get:NotNull(
        message = "状态不能为空",
        groups = [
            TurboPlanGroup.UpdateDetail::class,
            TurboPlanGroup.UpdateAll::class
        ]
    )
    val openStatus: Boolean?
)
