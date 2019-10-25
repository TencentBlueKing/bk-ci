package com.tencent.devops.store.pojo.ideatom

import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("更新IDE插件请求报文体")
data class IdeAtomUpdateRequest(
    @ApiModelProperty("插件名称", required = false)
    val atomName: String?,
    @ApiModelProperty("插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = false)
    val atomType: IdeAtomTypeEnum?,
    @ApiModelProperty("所属分类代码", required = false)
    val classifyCode: String?,
    @ApiModelProperty("版本日志内容", required = false)
    val versionContent: String?,
    @ApiModelProperty("插件logo地址", required = false)
    val logoUrl: String?,
    @ApiModelProperty("插件简介", required = false)
    val summary: String?,
    @ApiModelProperty("插件描述", required = false)
    val description: String?,
    @ApiModelProperty("发布者", required = false)
    val publisher: String?,
    @ApiModelProperty("是否为公共插件 true：公共插件 false：普通插件", required = false)
    val publicFlag: Boolean?,
    @ApiModelProperty("是否推荐， TRUE：是 FALSE：不是", required = false)
    val recommendFlag: Boolean?,
    @ApiModelProperty("权重（数值越大代表权重越高）", required = false)
    val weight: Int?,
    @ApiModelProperty("插件标签列表", required = false)
    val labelIdList: ArrayList<String>?,
    @ApiModelProperty("应用范畴列表", required = false)
    val categoryIdList: ArrayList<String>?,
    @ApiModelProperty(value = "插件项目可视范围", required = false)
    val visibilityLevel: VisibilityLevelEnum?,
    @ApiModelProperty(value = "插件代码库不开源原因", required = false)
    val privateReason: String?
)