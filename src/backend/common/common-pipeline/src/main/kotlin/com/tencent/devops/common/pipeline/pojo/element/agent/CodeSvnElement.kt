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

package com.tencent.devops.common.pipeline.pojo.element.agent

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.SVNVersion
import com.tencent.devops.common.pipeline.enums.SvnDepth
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("拉取SVN仓库代码", description = CodeSvnElement.classType)
data class CodeSvnElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("代码库哈希ID", required = true)
    val repositoryHashId: String?,
    @ApiModelProperty("revision", required = false)
    var revision: String? = null,
    @ApiModelProperty("Checkout strategy", required = true)
    val strategy: CodePullStrategy? = CodePullStrategy.INCREMENT_UPDATE,
    @ApiModelProperty("代码存放路径", required = false)
    val path: String? = null,
    @ApiModelProperty("启动Submodule", required = false)
    val enableSubmodule: Boolean? = true,
    @ApiModelProperty("指定版本号", required = false)
    var specifyRevision: Boolean? = false,
    @ApiModelProperty("拉取仓库深度", required = false)
    val svnDepth: SvnDepth? = SvnDepth.infinity,
    @ApiModelProperty("SVN相对路径", required = false)
    val svnPath: String? = null,
    @ApiModelProperty("SVN的版本", required = false)
    val svnVersion: SVNVersion? = null,
    @ApiModelProperty("新版的svn原子的类型")
    val repositoryType: RepositoryType? = null,
    @ApiModelProperty("新版的svn代码库名")
    val repositoryName: String? = null
) : Element(name, id, status) {

    companion object {
        const val classType = "CODE_SVN"
        const val REPO_HASH_ID = "repositoryHashId"
        const val REPO_NAME = "repositoryName"
        const val REPO_TYPE = "repositoryType"
        const val BRANCH_NAME = "branchName"
        const val REVISION = "revision"
        const val STRATEGY = "strategy"
        const val PATH = "path"
        const val enableSubmodule = "enableSubmodule"
        const val enableVirtualMergeBranch = "enableVirtualMergeBranch"
        const val specifyRevision = "specifyRevision"
        const val svnDepth = "svnDepth"
        const val svnPath = "svnPath"
        const val svnVersion = "svnVersion"
    }

    override fun getClassType() = classType
}
