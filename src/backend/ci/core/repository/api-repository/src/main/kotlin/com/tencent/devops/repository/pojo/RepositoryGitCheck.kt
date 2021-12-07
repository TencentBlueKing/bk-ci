package com.tencent.devops.repository.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("git check 信息返回模型")
data class RepositoryGitCheck(
    @ApiModelProperty("git check id")
    val gitCheckId: Long,
    @ApiModelProperty("流水线id")
    val pipelineId: String,
    @ApiModelProperty("构建次数")
    val buildNumber: Int,
    @ApiModelProperty("仓库id")
    val repositoryId: String?,
    @ApiModelProperty("仓库名称")
    val repositoryName: String?,
    @ApiModelProperty("提交id")
    val commitId: String,
    @ApiModelProperty("内容")
    val context: String,
    @ApiModelProperty("来源类型")
    val source: ExecuteSource
)
