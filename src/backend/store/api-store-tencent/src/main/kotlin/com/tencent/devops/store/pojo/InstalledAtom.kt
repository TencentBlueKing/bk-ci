package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("已安装插件")
data class InstalledAtom(
    @ApiModelProperty("插件ID")
    val atomId: String,
    @ApiModelProperty("插件标识")
    val atomCode: String,
    @ApiModelProperty("插件名称")
    val name: String,
    @ApiModelProperty("logo地址")
    val logoUrl: String?,
    @ApiModelProperty("插件分类code")
    val classifyCode: String?,
    @ApiModelProperty("插件分类名称")
    val classifyName: String?,
    @ApiModelProperty("插件范畴")
    val category: String?,
    @ApiModelProperty("插件简介")
    val summary: String?,
    @ApiModelProperty("发布者")
    val publisher: String?,
    @ApiModelProperty("安装者")
    val installer: String,
    @ApiModelProperty("安装时间")
    val installTime: String,
    @ApiModelProperty("安装类型")
    val installType: String,
    @ApiModelProperty("流水线个数")
    val pipelineCnt: Int,
    @ApiModelProperty("是否有卸载权限")
    val hasPermission: Boolean
)