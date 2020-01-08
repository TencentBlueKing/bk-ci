package com.tencent.devops.store.pojo.vo

import io.swagger.annotations.ApiModelProperty

data class VersionLogVO(
    @ApiModelProperty("版本日志id")
    val id: String,
    @ApiModelProperty("扩展服务id")
    val serviceId: String,
    @ApiModelProperty("发布类型，0：新上架 1：非兼容性升级 2：兼容性功能更新 3：兼容性问题修正 ")
    val releaseType: String,
    @ApiModelProperty("版本日志内容")
    val content: String,
    @ApiModelProperty("添加人")
    val createUser: String,
    @ApiModelProperty("添加时间")
    val createTime: Long
)