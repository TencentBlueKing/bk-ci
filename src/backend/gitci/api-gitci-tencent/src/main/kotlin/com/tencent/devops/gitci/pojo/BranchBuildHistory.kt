package com.tencent.devops.gitci.pojo

import com.tencent.devops.gitci.pojo.enums.BranchType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("分支构建列表")
data class BranchBuildHistory(
    @ApiModelProperty("分支名")
    val branchName: String,
    @ApiModelProperty("构建总次数")
    val buildTotal: Long,
    @ApiModelProperty("分支类型(Default、Active、Inactive)")
    val branchType: BranchType,
    @ApiModelProperty("buildHistory")
    val buildHistory: List<GitCIBuildHistory>
)
