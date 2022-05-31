package com.tencent.devops.turbo.vo

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
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
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    var createdDate: LocalDateTime? = null,

    @ApiModelProperty("最近修改人")
    var updatedBy: String? = "",

    @ApiModelProperty("修改时间")
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    var updatedDate: LocalDateTime? = null
)
