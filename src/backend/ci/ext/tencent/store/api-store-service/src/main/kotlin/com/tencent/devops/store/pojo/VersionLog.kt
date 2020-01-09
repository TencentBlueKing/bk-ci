package com.tencent.devops.store.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class VersionLog(
    @ApiModelProperty("日志id", required = true)
    val logId: String,
    @ApiModelProperty("扩展服务id", required = true)
    val serviceId: String,
    @ApiModelProperty("发布类型，0：新上架 1：非兼容性升级 2：兼容性功能更新 3：兼容性问题修正 ", required = true)
    val releaseType: String,
    @ApiModelProperty("版本日志内容", required = true)
    val content: String,
    @ApiModelProperty("添加人")
    val createUser: String,
    @ApiModelProperty("添加时间")
    val createTime: String
)