package com.tencent.devops.gitci.pojo.task

import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.gitci.service.BuildConfig
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * gitCiCodeRepo
 */
@ApiModel("拉代码（GIT_CI工蜂专用）")
data class GitCiCodeRepoTask(
    @ApiModelProperty("displayName", required = false)
    override var displayName: String?,
    @ApiModelProperty("入参", required = true)
    override val inputs: GitCiCodeRepoInput,
    @ApiModelProperty("执行条件", required = true)
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {
    companion object {
        const val taskType = "gitCiCodeRepo"
        const val taskVersion = "@latest"
        const val atomCode = "gitCiCodeRepo"
    }

    override fun getTaskType() = taskType
    override fun getTaskVersion() = taskVersion

    override fun covertToElement(config: BuildConfig): MarketBuildAtomElement {
        return MarketBuildAtomElement(
                "拉代码",
                null,
                null,
                atomCode,
                "1.*",
                mapOf("input" to inputs)
        )
    }
}

@ApiModel("git工蜂ci拉取代码")
data class GitCiCodeRepoInput(
    @ApiModelProperty("工蜂仓库名称", required = true)
    val repositoryName: String,
    @ApiModelProperty("工蜂仓库URL", required = true)
    val repositoryUrl: String,
    @ApiModelProperty("oauthToken", required = true)
    val oauthToken: String,
    @ApiModelProperty("localPath", required = false)
    val localPath: String? = null,
    @ApiModelProperty("strategy", required = false)
    val strategy: CodePullStrategy = CodePullStrategy.REVERT_UPDATE,
    @ApiModelProperty("pullType", required = false)
    val pullType: GitPullModeType = GitPullModeType.BRANCH,
    @ApiModelProperty("oauthToken", required = false)
    val refName: String? = "master",
    @ApiModelProperty("pipelineStartType", required = false)
    val pipelineStartType: StartType = StartType.MANUAL,
    @ApiModelProperty("hookEventType", required = false)
    val hookEventType: String? = null,
    @ApiModelProperty("hookSourceBranch", required = false)
    val hookSourceBranch: String? = null,
    @ApiModelProperty("hookTargetBranch", required = false)
    val hookTargetBranch: String? = null,
    @ApiModelProperty("hookSourceUrl", required = false)
    val hookSourceUrl: String? = null,
    @ApiModelProperty("hookTargetUrl", required = false)
    val hookTargetUrl: String? = null,
    @ApiModelProperty("enableSubmodule", required = false)
    val enableSubmodule: Boolean = true,
    @ApiModelProperty("enableVirtualMergeBranch", required = false)
    val enableVirtualMergeBranch: Boolean = true,
    @ApiModelProperty("enableSubmoduleRemote", required = false)
    val enableSubmoduleRemote: Boolean = true,
    @ApiModelProperty("autoCrlf", required = false)
    val autoCrlf: Boolean = true
) : AbstractInput()