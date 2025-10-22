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
import com.tencent.devops.scm.enums.CodeSvnRegion
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "代码库模型-Code平台Svn")
data class CodeSvnRepository(
    @get:Schema(title = "代码库别名", required = true)
    override val aliasName: String,
    @get:Schema(title = "URL", required = true)
    override val url: String,
    @get:Schema(title = "凭据id", required = true)
    override val credentialId: String,
    @get:Schema(title = "SVN区域", required = true)
    val region: CodeSvnRegion? = CodeSvnRegion.TC,
    @get:Schema(title = "svn项目名称", example = "xx/yy_proj", required = true)
    override val projectName: String,
    @get:Schema(title = "用户名", required = true)
    override var userName: String,
    @get:Schema(title = "项目id", required = true)
    override var projectId: String?,
    @get:Schema(title = "仓库hash id", required = false)
    override val repoHashId: String?,
    @get:Schema(title = "SVN类型", required = false)
    val svnType: String? = SVN_TYPE_SSH, // default is ssh svn type
    @get:Schema(title = "仓库是否开启pac", required = false)
    override val enablePac: Boolean? = false,
    @get:Schema(title = "yaml同步状态", required = false)
    override val yamlSyncStatus: String? = null,
    @get:Schema(title = "代码库标识", required = false)
    override val scmCode: String = ScmType.CODE_SVN.name,
    @get:Schema(title = "凭证类型", required = false)
    val credentialType: String? = ""
) : Repository {

    companion object {
        const val classType = "codeSvn"
        const val SVN_TYPE_HTTP = "http"
        const val SVN_TYPE_SSH = "ssh"
    }

    override fun isLegal(): Boolean {
        if (svnType == SVN_TYPE_HTTP) {
            return url.startsWith("http://") ||
                url.startsWith("https://") || url.startsWith("svn://")
        }
        return url.startsWith(getStartPrefix())
    }

    override fun getFormatURL(): String {
        var fixUrl = url
        if (fixUrl.startsWith("svn+ssh://")) {
            val split = fixUrl.split("://")
            if (split.size == 2) {
                val index = split[1].indexOf("@")
                val suffix = if (index >= 0) {
                    split[1].substring(index + 1)
                } else {
                    split[1]
                }
                fixUrl = split[0] + "://" + suffix
            }
        }
        return fixUrl
    }

    override fun getStartPrefix() = "svn+ssh://"

    override fun getScmType() = ScmType.CODE_SVN

    override fun getExternalId(): String = projectName
}
