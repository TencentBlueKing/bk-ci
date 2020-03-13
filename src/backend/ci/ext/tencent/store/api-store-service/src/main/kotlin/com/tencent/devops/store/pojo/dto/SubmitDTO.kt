package com.tencent.devops.store.pojo.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SubmitDTO(
    @ApiModelProperty("扩展服务id", required = true)
    val serviceId: String,
    @ApiModelProperty("扩展服务Code")
    val serviceCode: String,
    @ApiModelProperty("扩展服务Name")
    val serviceName: String,
    @ApiModelProperty("所属分类")
    val classify: String? = null,
    @ApiModelProperty("插件标签列表")
    val labelIdList: List<String>?,
    @ApiModelProperty("服务版本")
    val version: String,
    @ApiModelProperty("LOGO url")
    val logoUrl: String?,
    @ApiModelProperty("ICON")
    val icon: String?,
    @ApiModelProperty("扩展服务简介")
    val sunmmary: String? = null,
    @ApiModelProperty("扩展服务描述")
    val description: String? = null,
    @ApiModelProperty("扩展服务发布者")
    val publisher: String?,
    @ApiModelProperty("是否是最后版本")
    val latestFlag: Int? = 0,
    @ApiModelProperty("发布类型，NEW：新上架 INCOMPATIBILITY_UPGRADE：非兼容性升级 COMPATIBILITY_UPGRADE：兼容性功能更新 COMPATIBILITY_FIX：兼容性问题修正", required = true)
    val releaseType: ReleaseTypeEnum,
    @ApiModelProperty("版本日志内容", required = true)
    val versionContent: String? = "",
    @ApiModelProperty("扩展点列表")
    val extensionItemList: Set<String>
)