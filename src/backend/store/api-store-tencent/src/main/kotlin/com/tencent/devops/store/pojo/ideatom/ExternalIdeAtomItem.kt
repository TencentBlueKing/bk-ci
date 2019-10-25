package com.tencent.devops.store.pojo.ideatom

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("IDE插件市场首页单项信息")
data class ExternalIdeAtomItem(
    @ApiModelProperty("插件ID", required = true)
    val atomId: String,
    @ApiModelProperty("插件名称", required = true)
    val atomName: String,
    @ApiModelProperty("插件代码", required = true)
    val atomCode: String,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty("插件logo地址", required = false)
    val logoUrl: String?,
    @ApiModelProperty("插件简介", required = false)
    val summary: String?,
    @ApiModelProperty("发布者", required = true)
    val publisher: String,
    @ApiModelProperty("下载量")
    val downloads: Int?,
    @ApiModelProperty("评分")
    val score: Double?,
    @ApiModelProperty("代码库链接")
    val codeSrc: String?,
    @ApiModelProperty("权重")
    val weight: Int?
)