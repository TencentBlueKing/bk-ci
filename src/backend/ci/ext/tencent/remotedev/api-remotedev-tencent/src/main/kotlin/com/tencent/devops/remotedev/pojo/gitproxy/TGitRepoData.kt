package com.tencent.devops.remotedev.pojo.gitproxy

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("关联的工蜂仓库的信息")
data class TGitRepoData(
    @ApiModelProperty("工蜂仓库ID")
    val repoId: Long,
    @ApiModelProperty("仓库URL")
    var url: String,
    @ApiModelProperty("关联状态 TO_BE_MIGRATED|AVAILABLE|ABNORMAL")
    val status: TGitRepoStatus
)
