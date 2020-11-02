package com.tencent.devops.experience.pojo.index

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-首页-APP信息")
data class IndexAppInfoVO(
    @ApiModelProperty("版本体验ID", required = true)
    val experienceHashId: String,
    @ApiModelProperty("体验名称", required = true)
    val experienceName: String,
    @ApiModelProperty("创建时间", required = true)
    val createTime: Long,
    @ApiModelProperty("大小(byte)", required = true)
    val size: Long,
    @ApiModelProperty("下载链接", required = true)
    val url: String,
    @ApiModelProperty("logo链接", required = true)
    val logoUrl: String
)