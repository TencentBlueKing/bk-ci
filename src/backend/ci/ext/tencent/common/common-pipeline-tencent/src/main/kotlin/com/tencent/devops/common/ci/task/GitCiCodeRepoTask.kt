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
import io.swagger.v3.oas.annotations.media.Schema

/**
 * gitCiCodeRepo
 */
@Schema(description = "拉代码（GIT_CI工蜂专用）")
data class GitCiCodeRepoTask(
    @Schema(description = "displayName", required = false)
    override var displayName: String?,
    @Schema(description = "入参", required = true)
    override val inputs: GitCiCodeRepoInput,
    @Schema(description = "执行条件", required = true)
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

@Schema(description = "git工蜂ci拉取代码")
data class GitCiCodeRepoInput(
    @Schema(description = "工蜂仓库名称", required = true)
    val repositoryName: String,
    @Schema(description = "工蜂仓库URL", required = true)
    val repositoryUrl: String,
    @Schema(description = "oauthToken", required = true)
    val oauthToken: String,
    @Schema(description = "localPath", required = false)
    val localPath: String? = null,
    @Schema(description = "strategy", required = false)
    val strategy: CodePullStrategy = CodePullStrategy.REVERT_UPDATE,
    @Schema(description = "pullType", required = false)
    val pullType: GitPullModeType = GitPullModeType.BRANCH,
    @Schema(description = "refName", required = false)
    val refName: String? = "master",
    @Schema(description = "pipelineStartType", required = false)
    val pipelineStartType: StartType = StartType.MANUAL,
    @Schema(description = "hookEventType", required = false)
    val hookEventType: String? = null,
    @Schema(description = "hookSourceBranch", required = false)
    val hookSourceBranch: String? = null,
    @Schema(description = "hookTargetBranch", required = false)
    val hookTargetBranch: String? = null,
    @Schema(description = "hookSourceUrl", required = false)
    val hookSourceUrl: String? = null,
    @Schema(description = "hookTargetUrl", required = false)
    val hookTargetUrl: String? = null,
    @Schema(description = "enableSubmodule", required = false)
    val enableSubmodule: Boolean = true,
    @Schema(description = "enableVirtualMergeBranch", required = false)
    val enableVirtualMergeBranch: Boolean = true,
    @Schema(description = "enableSubmoduleRemote", required = false)
    val enableSubmoduleRemote: Boolean = true,
    @Schema(description = "autoCrlf", required = false)
    val autoCrlf: Boolean = true
) : AbstractInput()
