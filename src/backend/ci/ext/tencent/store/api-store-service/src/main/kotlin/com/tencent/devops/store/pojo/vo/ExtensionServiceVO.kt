package com.tencent.devops.store.pojo.vo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExtensionServiceVO(
    @ApiModelProperty("扩展服务Id")
    val serviceId: String,
    @ApiModelProperty("扩展服务code")
    val serviceCode: String,
    @ApiModelProperty("扩展服务Name")
    val serviceName: String,
    @ApiModelProperty("所属分类")
    val category: String,
    @ApiModelProperty("服务版本")
    val version: String,
    @ApiModelProperty("状态")
    val status: Int,
    @ApiModelProperty("状态对应的描述")
    val statusMsg: String?,
    @ApiModelProperty("LOGO url")
    val logoUrl: String?,
    @ApiModelProperty("ICON")
    val icon: String?,
    @ApiModelProperty("扩展服务简介")
    val sunmmary: String?,
    @ApiModelProperty("扩展服务描述")
    val description: String?,
    @ApiModelProperty("扩展服务发布者")
    val publisher: String?,
    @ApiModelProperty("发布时间")
    val publishTime: Long,
    @ApiModelProperty("是否是最后版本")
    val latestFlag: Int,
    @ApiModelProperty("删除标签")
    val deleteFlag: Int,
    @ApiModelProperty("添加用户")
    val creatorUser: String,
    @ApiModelProperty("修改用户")
    val modifierUser: String,
    @ApiModelProperty("添加时间")
    val creatorTime: Long,
    @ApiModelProperty("修改时间")
    val modifierTime: Long
)