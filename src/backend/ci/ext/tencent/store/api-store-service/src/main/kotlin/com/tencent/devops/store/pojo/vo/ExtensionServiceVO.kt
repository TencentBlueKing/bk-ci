package com.tencent.devops.store.pojo.vo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExtensionServiceVO(
    @ApiModelProperty("扩展服务Id")
    val serviceId: String,
    @ApiModelProperty("扩展服务名称")
    val name: String,
    @ApiModelProperty("扩展服务code")
    val serviceCode: String,
    @ApiModelProperty("版本")
    val version: String,
    @ApiModelProperty("调试项目名称")
    val itemName: String,
    @ApiModelProperty("调试项目Id")
    val itemId: String,
    @ApiModelProperty("扩展服务Name")
    val serviceName: String,
    @ApiModelProperty("所属分类")
    val category: String,
    @ApiModelProperty("状态")
    val serviceStatus: Int,
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