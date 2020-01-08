package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModelProperty

data class ExtServiceVersionLogCreateInfo (
    @ApiModelProperty("扩展服务ID")
    val serviceId: String,
    @ApiModelProperty("发布类型，0：新上架 1：非兼容性升级 2：兼容性功能更新 3：兼容性问题修正  ")
    val releaseType: Int,
    @ApiModelProperty("版本日志内容")
    val content: String,
    @ApiModelProperty("添加用户")
    val creatorUser: String,
    @ApiModelProperty("修改用户")
    val modifierUser: String
)