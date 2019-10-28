package com.tencent.devops.store.pojo.ideatom

import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.pojo.atom.enums.ReleaseTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("新增IDE插件请求报文体")
data class IdeAtomCreateRequest(
    @ApiModelProperty("插件名称", required = true)
    val atomName: String,
    @ApiModelProperty("插件代码", required = true)
    val atomCode: String,
    @ApiModelProperty("所属分类代码", required = true)
    val classifyCode: String,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty("发布类型，NEW：新上架 INCOMPATIBILITY_UPGRADE：非兼容性升级 COMPATIBILITY_UPGRADE：兼容性功能更新 COMPATIBILITY_FIX：兼容性问题修正", required = true)
    val releaseType: ReleaseTypeEnum,
    @ApiModelProperty("版本日志内容", required = true)
    val versionContent: String,
    @ApiModelProperty("插件logo地址", required = false)
    val logoUrl: String?,
    @ApiModelProperty("插件简介", required = false)
    val summary: String?,
    @ApiModelProperty("插件描述", required = false)
    val description: String?,
    @ApiModelProperty("发布者", required = true)
    val publisher: String,
    @ApiModelProperty("插件标签列表", required = false)
    val labelIdList: ArrayList<String>?,
    @ApiModelProperty("应用范畴列表", required = true)
    val categoryIdList: ArrayList<String>,
    @ApiModelProperty(value = "插件项目可视范围", required = true)
    val visibilityLevel: VisibilityLevelEnum = VisibilityLevelEnum.LOGIN_PUBLIC,
    @ApiModelProperty(value = "插件代码库不开源原因", required = false)
    val privateReason: String?
)