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
@Schema(title = "拉代码（GIT_CI工蜂专用）")
data class GitCiCodeRepoTask(
    @Schema(title = "displayName", required = false)
    override var displayName: String?,
    @Schema(title = "入参", required = true)
    override val inputs: GitCiCodeRepoInput,
    @Schema(title = "执行条件", required = true)
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

@Schema(title = "git工蜂ci拉取代码")
data class GitCiCodeRepoInput(
    @Schema(title = "工蜂仓库名称", required = true)
    val repositoryName: String,
    @Schema(title = "工蜂仓库URL", required = true)
    val repositoryUrl: String,
    @Schema(title = "oauthToken", required = true)
    val oauthToken: String,
    @Schema(title = "localPath", required = false)
    val localPath: String? = null,
    @Schema(title = "strategy", required = false)
    val strategy: CodePullStrategy = CodePullStrategy.REVERT_UPDATE,
    @Schema(title = "pullType", required = false)
    val pullType: GitPullModeType = GitPullModeType.BRANCH,
    @Schema(title = "refName", required = false)
    val refName: String? = "master",
    @Schema(title = "pipelineStartType", required = false)
    val pipelineStartType: StartType = StartType.MANUAL,
    @Schema(title = "hookEventType", required = false)
    val hookEventType: String? = null,
    @Schema(title = "hookSourceBranch", required = false)
    val hookSourceBranch: String? = null,
    @Schema(title = "hookTargetBranch", required = false)
    val hookTargetBranch: String? = null,
    @Schema(title = "hookSourceUrl", required = false)
    val hookSourceUrl: String? = null,
    @Schema(title = "hookTargetUrl", required = false)
    val hookTargetUrl: String? = null,
    @Schema(title = "enableSubmodule", required = false)
    val enableSubmodule: Boolean = true,
    @Schema(title = "enableVirtualMergeBranch", required = false)
    val enableVirtualMergeBranch: Boolean = true,
    @Schema(title = "enableSubmoduleRemote", required = false)
    val enableSubmoduleRemote: Boolean = true,
    @Schema(title = "autoCrlf", required = false)
    val autoCrlf: Boolean = true
) : AbstractInput()
