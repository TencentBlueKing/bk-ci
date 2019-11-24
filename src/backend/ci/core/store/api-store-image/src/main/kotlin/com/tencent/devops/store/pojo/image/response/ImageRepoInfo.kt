package com.tencent.devops.store.pojo.image.response

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @Description
 * @Date 2019/9/17
 * @Version 1.0
 */
@ApiModel("镜像详情")
data class ImageRepoInfo(

    @ApiModelProperty("镜像ID", required = true)
    val id: String,

    @ApiModelProperty("镜像代码", required = true)
    val code: String,

    @ApiModelProperty("镜像名称", required = true)
    val name: String,

    @ApiModelProperty("镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = true)
    val sourceType: String,

    @ApiModelProperty("镜像仓库Url", required = true)
    val repoUrl: String,

    @ApiModelProperty("镜像仓库名称", required = true)
    val repoName: String,

    @ApiModelProperty("镜像tag", required = true)
    val tag: String,

    @ApiModelProperty("凭证Id", required = true)
    val ticketId: String
)