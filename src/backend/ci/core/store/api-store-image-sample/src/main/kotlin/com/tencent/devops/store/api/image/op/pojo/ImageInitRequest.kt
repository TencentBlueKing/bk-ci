package com.tencent.devops.store.api.image.op.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("第三方镜像市场初始化参数")
data class ImageInitRequest(
    @ApiModelProperty("所属项目ID", required = true)
    val projectCode: String,
    @ApiModelProperty("所属项目描述", required = true)
    val projectDesc: String?,
    @ApiModelProperty("拥有者用户ID", required = true)
    val userId: String,
    @ApiModelProperty("市场镜像ID", required = true)
    val imageCode: String,
    @ApiModelProperty("镜像鉴权accessToken", required = false)
    val accessToken: String?,
    @ApiModelProperty("凭据ID", required = false)
    val ticketId: String?,
    @ApiModelProperty("镜像简介", required = false)
    val summary: String?,
    @ApiModelProperty("镜像描述", required = false)
    val description: String?,
    @ApiModelProperty("logo地址", required = false)
    val logoUrl: String?,
    @ApiModelProperty("自定义icon图标字符串", required = false)
    val iconData: String?,
    @ApiModelProperty("镜像仓库地址", required = false)
    val imageRepoUrl: String?,
    @ApiModelProperty("镜像仓库名", required = false)
    val imageRepoName: String?,
    @ApiModelProperty("镜像标签", required = false)
    val imageTag: String?,
    @ApiModelProperty("镜像DockerFile类型", required = false)
    val dockerFileType: String?,
    @ApiModelProperty("镜像DockerFile内容", required = false)
    val dockerFileContent: String?,
    @ApiModelProperty("镜像版本", required = false)
    val versionContent: String?
)
