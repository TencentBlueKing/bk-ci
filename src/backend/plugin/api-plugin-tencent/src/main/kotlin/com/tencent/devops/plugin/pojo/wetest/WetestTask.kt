package com.tencent.devops.plugin.pojo.wetest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("WeTest测试任务")
data class WetestTask(
    @ApiModelProperty("ID")
    val id: Int,
    @ApiModelProperty("项目Id")
    val projectId: String,
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("手机类别")
    val mobileCategory: String,
    @ApiModelProperty("手机类别ID")
    val mobileCategoryId: String,
    @ApiModelProperty("选择机型")
    val mobileModel: String,
    @ApiModelProperty("选择机型ID")
    val mobileModelId: String,
    @ApiModelProperty("描述")
    val description: String?,
    @ApiModelProperty("凭证ID")
    val ticketsId: String?,
    @ApiModelProperty("创建时间")
    val createTime: String,
    @ApiModelProperty("修改时间")
    val updateTime: String

)
