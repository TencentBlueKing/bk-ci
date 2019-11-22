package com.tencent.devops.gitci.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂CI服务配置")
data class GitCIServicesConf(
    @ApiModelProperty("ID")
    val id: Long,
    @ApiModelProperty("镜像名称")
    val imageName: String,
    @ApiModelProperty("镜像标签")
    val imageTag: String,
    @ApiModelProperty("镜像仓库地址")
    val repoUrl: String,
    @ApiModelProperty("镜像仓库登录用户")
    val repoUsername: String?,
    @ApiModelProperty("镜像仓库登录密码")
    val repoPwd: String?,
    @ApiModelProperty("是否启用")
    val enable: Boolean,
    @ApiModelProperty("环境变量")
    val env: String?,
    @ApiModelProperty("创建者")
    val createUser: String?,
    @ApiModelProperty("修改者")
    val updateUser: String?,
    @ApiModelProperty("创建时间")
    val createTime: String?,
    @ApiModelProperty("修改时间")
    val updateTime: String?
)