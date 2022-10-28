package com.tencent.devops.store.pojo.atom

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("插件发布部署模型")
data class ReleaseInfo(
    @ApiModelProperty("项目编码", required = true)
    var projectId: String,
    @ApiModelProperty("插件代码", required = true)
    @field:BkField(patternStyle = BkStyleEnum.CODE_STYLE)
    var atomCode: String,
    @ApiModelProperty("插件名称", required = true)
    @field:BkField(patternStyle = BkStyleEnum.NAME_STYLE)
    var name: String,
    @ApiModelProperty("开发语言", required = true)
    @field:BkField(patternStyle = BkStyleEnum.LANGUAGE_STYLE)
    var language: String,
    @ApiModelProperty("插件logo地址", required = true)
    @field:BkField(maxLength = 1024)
    var logoUrl: String,
    @ApiModelProperty("支持的操作系统", required = true)
    val os: ArrayList<String>,
    @ApiModelProperty(value = "前端UI渲染方式", required = true)
    val frontendType: FrontendTypeEnum = FrontendTypeEnum.NORMAL,
    @ApiModelProperty("插件所属范畴", required = true)
    val category: AtomCategoryEnum,
    @ApiModelProperty("所属插件分类代码", required = true)
    val classifyCode: String,
    @ApiModelProperty("适用Job类型", required = true)
    val jobType: JobTypeEnum,
    @JsonProperty(value = "labelIdList", required = false)
    @ApiModelProperty("标签id集合", name = "labelIdList")
    val labelIdList: ArrayList<String>? = null,
    @ApiModelProperty("版本信息", required = true)
    val versionInfo: VersionInfo,
    @ApiModelProperty("插件简介", required = true)
    @field:BkField(maxLength = 1024)
    val summary: String,
    @ApiModelProperty("插件描述", required = true)
    @field:BkField(maxLength = 1024)
    var description: String
)
