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

package com.tencent.devops.common.pipeline.pojo.element.agent

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.git.GitPullMode
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "拉取Github仓库代码", description = GithubElement.classType)
data class GithubElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "代码库哈希ID", required = true)
    val repositoryHashId: String?,
    @get:Schema(title = "checkout 策略", required = false)
    val strategy: CodePullStrategy? = CodePullStrategy.INCREMENT_UPDATE,
    @get:Schema(title = "代码存放路径", required = false)
    val path: String? = null,
    @get:Schema(title = "启动Submodule", required = false)
    val enableSubmodule: Boolean? = true,
    @get:Schema(title = "revision 用于强制指定commitId", required = false)
    var revision: String? = null,
    @get:Schema(title = "指定拉取方式", required = false)
    val gitPullMode: GitPullMode?,
    @get:Schema(title = "支持虚拟合并分支", required = false)
    val enableVirtualMergeBranch: Boolean? = false,
    @get:Schema(title = "新版的github原子的类型")
    val repositoryType: RepositoryType? = null,
    @get:Schema(title = "新版的github代码库名")
    val repositoryName: String? = null
) : Element(name, id, status) {

    companion object {
        const val classType = "GITHUB"
        const val modeType = "mode.type"
        const val modeValue = "mode.value"
    }

    override fun genTaskParams(): MutableMap<String, Any> {
        val paramMap = JsonUtil.toMutableMap(this)
        if (gitPullMode != null) { // 这个是为了方便构建机用的是Map，在运行时可直接key使用
            paramMap[modeType] = gitPullMode.type.name
            paramMap[modeValue] = gitPullMode.value
        }
        return paramMap
    }

    override fun getClassType() = classType
}
