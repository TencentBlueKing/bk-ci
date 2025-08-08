/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.repository.pojo

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.scm.utils.code.git.GitUtils
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "代码库模型-Code平台Git")
data class CodeGitRepository(
    @get:Schema(title = "代码库别名", required = true)
    override val aliasName: String,
    @get:Schema(title = "URL", required = true)
    override val url: String,
    @get:Schema(title = "凭据id(该凭证需要有git仓库Reporter以上权限)", required = true)
    override val credentialId: String,
    @get:Schema(title = "git项目名称", example = "devops/devops_ci_example_proj", required = true)
    override val projectName: String,
    @get:Schema(title = "用户名", required = true)
    override var userName: String,
    @get:Schema(title = "仓库认证类型", required = false)
    val authType: RepoAuthType? = RepoAuthType.SSH,
    @get:Schema(title = "项目id", required = true)
    override var projectId: String?,
    @get:Schema(title = "仓库hash id", required = false)
    override val repoHashId: String?,
    @get:Schema(title = "Git仓库ID", required = false)
    val gitProjectId: Long?,
    @get:Schema(title = "是否为插件库", required = false)
    val atom: Boolean? = false,
    @get:Schema(title = "仓库是否开启pac", required = false)
    override val enablePac: Boolean? = false,
    @get:Schema(title = "yaml同步状态", required = false)
    override val yamlSyncStatus: String? = null,
    @get:Schema(title = "代码库标识", required = false)
    override val scmCode: String = ScmType.CODE_GIT.name,
    @get:Schema(title = "凭证类型", required = false)
    val credentialType: String? = ""
) : Repository {
    companion object {
        const val classType = "codeGit"
    }

    override fun getStartPrefix(): String {
        return when (authType) {
            RepoAuthType.SSH -> "git@"
            RepoAuthType.OAUTH -> "http://"
            RepoAuthType.HTTP -> "http://"
            RepoAuthType.HTTPS -> "https://"
            else -> "git@"
        }
    }

    override fun isLegal(): Boolean {
        return when (authType) {
            RepoAuthType.HTTP, RepoAuthType.OAUTH, RepoAuthType.HTTPS ->
                GitUtils.isLegalHttpUrl(url)
            else ->
                GitUtils.isLegalSshUrl(url)
        }
    }

    override fun getScmType() = ScmType.CODE_GIT

    override fun getExternalId(): String = gitProjectId?.toString() ?: ""
}
