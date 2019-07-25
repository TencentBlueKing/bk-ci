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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.pojo

import com.tencent.devops.repository.pojo.enums.RepoAuthType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("代码库模型-Code平台TGit")
data class CodeTGitRepository(
    @ApiModelProperty("代码库别名", required = true)
    override val aliasName: String,
    @ApiModelProperty("URL", required = true)
    override val url: String,
    @ApiModelProperty("凭据id", required = true)
    override val credentialId: String,
    @ApiModelProperty("tGit项目名称", example = "xx/yy_ci_example_proj", required = true)
    override val projectName: String,
    @ApiModelProperty("用户名", required = true)
    override val userName: String,
    @ApiModelProperty("仓库认证类型", required = false)
    val authType: RepoAuthType? = RepoAuthType.SSH,
    @ApiModelProperty("项目id", required = true)
    override val projectId: String?,
    override val repoHashId: String?
) : Repository {
    companion object {
        const val classType = "codeTGit"
    }

    //    override fun getStartPrefix() = "git@git.tencent.com"
    override fun getStartPrefix(): String {
        return when (authType) {
            RepoAuthType.SSH -> "git@git.tencent.com"
            RepoAuthType.OAUTH -> "http://git.tencent.com"
            RepoAuthType.HTTP -> ("http://git.tencent.com")
            RepoAuthType.HTTPS -> ("https://git.tencent.com")
            else -> "git@git.tencent.com"
        }
    }

    override fun isLegal(): Boolean {
        return when (authType) {
            RepoAuthType.SSH -> url.startsWith("git@git.tencent.com")
            RepoAuthType.OAUTH -> url.startsWith("http://git.tencent.com")
            RepoAuthType.HTTP -> url.startsWith("http://git.tencent.com")
            RepoAuthType.HTTPS -> url.startsWith("https://git.tencent.com")
            else -> false
        }
    }
}