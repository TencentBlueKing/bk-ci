package com.tencent.devops.experience.pojo.index

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-首页-Banner")
data class IndexBannerVO(
    @ApiModelProperty("版本体验ID", required = true)
    val experienceHashId: String,
    @ApiModelProperty("BannerUrl", required = true)
    val bannerUrl: String
)