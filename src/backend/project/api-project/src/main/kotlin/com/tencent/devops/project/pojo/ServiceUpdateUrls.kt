package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @author marcusfeng,  Date on 2019-08-01.
 */
@ApiModel("服务-更新Urls模型")
data class ServiceUpdateUrls(
    @ApiModelProperty("cssUrl")
    val cssUrl: String?,
    @ApiModelProperty("jsUrl")
    val jsUrl: String?,
    @ApiModelProperty("grayCssUrl")
    val grayCssUrl: String?,
    @ApiModelProperty("grayJsUrl")
    val grayJsUrl: String?
)