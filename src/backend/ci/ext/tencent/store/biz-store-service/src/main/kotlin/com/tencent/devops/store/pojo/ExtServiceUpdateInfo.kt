package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModelProperty

data class ExtServiceUpdateInfo(
    @ApiModelProperty("扩展服务Name")
    val serviceName: String? = null,
    @ApiModelProperty("所属分类")
    val category: String? = null,
    @ApiModelProperty("服务版本")
    val version: String? = null,
    @ApiModelProperty("状态")
    val status: Int?,
    @ApiModelProperty("状态对应的描述")
    val statusMsg: String? = null,
    @ApiModelProperty("LOGO url")
    val logoUrl: String? = null,
    @ApiModelProperty("ICON")
    val icon: String? = null,
    @ApiModelProperty("扩展服务简介")
    val sunmmary: String? = null,
    @ApiModelProperty("扩展服务描述")
    val description: String? = null,
    @ApiModelProperty("扩展服务发布者")
    val publisher: String? = null,
    @ApiModelProperty("发布时间")
    val publishTime: Long? = 0,
    @ApiModelProperty("是否是最后版本")
    val latestFlag: Boolean? = true,
    @ApiModelProperty("修改用户")
    val modifierUser: String
)