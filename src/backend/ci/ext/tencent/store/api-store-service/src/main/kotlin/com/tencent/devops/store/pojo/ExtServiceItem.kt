package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扩展服务首页信息")
data class ExtServiceItem(
    @ApiModelProperty("ID")
    val id: String,
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("标识")
    val code: String,
    @ApiModelProperty("分类")
    val classifyCode: String?,
    @ApiModelProperty("logo链接")
    val logoUrl: String?,
    @ApiModelProperty("发布者")
    val publisher: String,
    @ApiModelProperty("下载量")
    val downloads: Int?,
    @ApiModelProperty("评分")
    val score: Double?,
    @ApiModelProperty("简介")
    val summary: String?,
    @ApiModelProperty("是否有权限安装标识")
    val flag: Boolean,
    @ApiModelProperty("是否公共标识")
    val publicFlag: Boolean,
    @ApiModelProperty("是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null
)