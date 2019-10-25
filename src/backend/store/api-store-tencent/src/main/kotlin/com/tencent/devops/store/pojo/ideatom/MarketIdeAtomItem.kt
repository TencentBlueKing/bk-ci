package com.tencent.devops.store.pojo.ideatom

import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("IDE插件市场首页单项信息")
data class MarketIdeAtomItem(
    @ApiModelProperty("插件ID", required = true)
    val id: String,
    @ApiModelProperty("插件名称", required = true)
    val name: String,
    @ApiModelProperty("插件代码", required = true)
    val code: String,
    @ApiModelProperty("插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = true)
    val rdType: IdeAtomTypeEnum?,
    @ApiModelProperty("所属分类代码", required = true)
    val classifyCode: String?,
    @ApiModelProperty("插件logo地址", required = false)
    val logoUrl: String?,
    @ApiModelProperty("插件简介", required = false)
    val summary: String?,
    @ApiModelProperty("发布者", required = true)
    val publisher: String,
    @ApiModelProperty("发布时间", required = true)
    val pubTime: String?,
    @ApiModelProperty("是否为最新版本插件 true：最新 false：非最新", required = true)
    val latestFlag: Boolean,
    @ApiModelProperty("是否为公共插件 true：公共插件 false：普通插件", required = true)
    val publicFlag: Boolean,
    @ApiModelProperty("是否推荐， TRUE：是 FALSE：不是", required = true)
    val recommendFlag: Boolean,
    @ApiModelProperty("是否有权限安装标识")
    val flag: Boolean,
    @ApiModelProperty("创建人", required = true)
    val creator: String,
    @ApiModelProperty("创建时间", required = true)
    val createTime: String,
    @ApiModelProperty("修改人", required = true)
    val modifier: String,
    @ApiModelProperty("修改时间", required = true)
    val updateTime: String,
    @ApiModelProperty("下载量")
    val downloads: Int?,
    @ApiModelProperty("评分")
    val score: Double?,
    @ApiModelProperty("权重")
    val weight: Int?
)