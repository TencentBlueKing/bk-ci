package com.tencent.devops.stream.v1.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("仓库中所有已构建的分支列表")
data class V1GitCIBuildBranch(
    @ApiModelProperty("分支名")
    val branchName: String,
    @ApiModelProperty("项目ID")
    val gitProjectId: Long,
    @ApiModelProperty("源项目ID")
    val sourceGitProjectId: Long?
)
