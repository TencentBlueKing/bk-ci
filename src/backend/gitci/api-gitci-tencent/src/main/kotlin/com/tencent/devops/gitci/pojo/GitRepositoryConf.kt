package com.tencent.devops.gitci.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("蓝盾工蜂项目配置")
data class GitRepositoryConf(
    @ApiModelProperty("工蜂项目ID")
    override val gitProjectId: Long,
    @ApiModelProperty("工蜂项目名")
    override val name: String,
    @ApiModelProperty("工蜂项目url")
    override val url: String,
    @ApiModelProperty("homepage")
    override val homepage: String,
    @ApiModelProperty("gitHttpUrl")
    override val gitHttpUrl: String,
    @ApiModelProperty("gitSshUrl")
    override val gitSshUrl: String,
    @ApiModelProperty("是否启用CI")
    val enableCi: Boolean,
    @ApiModelProperty("Build pushed branches")
    val buildPushedBranches: Boolean = true,
    @ApiModelProperty("Limit concurrent jobs")
    val limitConcurrentJobs: Int?,
    @ApiModelProperty("Build pushed pull request")
    val buildPushedPullRequest: Boolean = true,
    @ApiModelProperty("Auto cancel branch builds")
    val autoCancelBranchBuilds: Boolean = false,
    @ApiModelProperty("Auto cancel pull request builds")
    val autoCancelPullRequestBuilds: Boolean = false,
    @ApiModelProperty("Environment variable")
    val env: List<EnvironmentVariables>?,
    @ApiModelProperty("创建时间")
    val createTime: Long?,
    @ApiModelProperty("修改时间")
    val updateTime: Long?,
    @ApiModelProperty("蓝盾项目Code")
    val projectCode: String?
) : Repository(gitProjectId, name, url, homepage, gitHttpUrl, gitSshUrl)
