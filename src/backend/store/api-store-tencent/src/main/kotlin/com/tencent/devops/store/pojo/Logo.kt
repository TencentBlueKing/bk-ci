package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("logo信息")
data class Logo(
    @ApiModelProperty("logoID", required = true)
    val id: String,
    @ApiModelProperty("logo链接", required = true)
    val logoUrl: String,
    @ApiModelProperty("类别 ATOM:原子 TEMPLATE:模板 BANNER:banner", required = true)
    val logoType: String,
    @ApiModelProperty("展示顺序", required = true)
    val order: Int,
    @ApiModelProperty("点击logo后的跳转链接")
    val link: String?,
    @ApiModelProperty("创建日期")
    val createTime: String,
    @ApiModelProperty("更新日期")
    val updateTime: String,
    @ApiModelProperty("创建人", required = true)
    val creator: String,
    @ApiModelProperty("最近修改人", required = true)
    val modifier: String
)