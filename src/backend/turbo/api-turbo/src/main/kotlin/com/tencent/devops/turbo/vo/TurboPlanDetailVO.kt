package com.tencent.devops.turbo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("加速方案详情视图")
data class TurboPlanDetailVO(
    @ApiModelProperty("方案id")
    var planId: String = "",

    @ApiModelProperty("方案名称")
    var planName: String = "",

    @ApiModelProperty("项目id")
    var projectId: String = "",

    @ApiModelProperty("加速模式")
    var engineCode: String = "",

    @ApiModelProperty("引擎名称")
    var engineName: String? = "",

    @ApiModelProperty("方案说明")
    var desc: String? = "",

    @ApiModelProperty("加速参数")
    var configParam: Map<String, Any?>? = null,

    @ApiModelProperty("IP白名单")
    var whiteList: String = "",

    @ApiModelProperty("编译加速任务启用状态")
    var openStatus: Boolean = true,

    @ApiModelProperty("创建人")
    var createdBy: String? = "",

    @ApiModelProperty("创建时间")
    var createdDate: LocalDateTime? = null,

    @ApiModelProperty("最近修改人")
    var updatedBy: String? = "",

    @ApiModelProperty("修改时间")
    var updatedDate: LocalDateTime? = null
)
