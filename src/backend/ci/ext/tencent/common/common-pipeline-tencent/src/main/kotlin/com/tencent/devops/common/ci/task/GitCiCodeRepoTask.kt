/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.ci.task

import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
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

    override fun covertToElement(config: CiBuildConfig): MarketBuildAtomElement {
        return MarketBuildAtomElement(
            name = displayName ?: "拉代码",
            id = null,
            status = null,
            atomCode = atomCode,
            version = "1.*",
            data = mapOf("input" to inputs)
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
    @ApiModelProperty("refName", required = false)
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
