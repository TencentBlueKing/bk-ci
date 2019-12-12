package com.tencent.devops.store.pojo.image.response

import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @Description
 * @Date 2019/9/17
 * @Version 1.0
 */
@ApiModel("镜像详情")
data class ImageRepoInfo(

    @ApiModelProperty("镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = true)
    val sourceType: ImageType,

    @ApiModelProperty("镜像仓库地址", required = true)
    val repoUrl: String,

    @ApiModelProperty("镜像在仓库中的路径", required = true)
    val repoName: String,

    @ApiModelProperty("镜像Tag", required = true)
    val repoTag: String,

    @ApiModelProperty("凭证Id", required = true)
    val ticketId: String,

    @ApiModelProperty("初始化凭证的项目", required = true)
    val ticketProject: String,

    @ApiModelProperty("是否为公共镜像 true：是 false：否", required = true)
    val publicFlag: Boolean,

    @ApiModelProperty("研发来源")
    val rdType: ImageRDTypeEnum
)