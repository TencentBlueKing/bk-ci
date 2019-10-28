package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("store成员")
data class StoreMemberItem(
    @ApiModelProperty("ID")
    val id: String,
    @ApiModelProperty("成员名称")
    val userName: String,
    @ApiModelProperty("成员类型")
    val type: String,
    @ApiModelProperty("所属调试项目名称")
    val projectName: String,
    @ApiModelProperty("创建人")
    val creator: String,
    @ApiModelProperty("修改人")
    val modifier: String,
    @ApiModelProperty("创建时间")
    val createTime: String,
    @ApiModelProperty("修改时间")
    val updateTime: String
)